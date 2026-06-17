package com.linkedinagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedinagent.domain.ContentCreationOutput;
import com.linkedinagent.domain.GeminiPostContent;
import com.linkedinagent.domain.PostContent;
import com.linkedinagent.entity.GeneratedPost;
import com.linkedinagent.entity.LearningPattern;
import com.linkedinagent.entity.ResearchData;
import com.linkedinagent.entity.Topic;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.exception.AgentException;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.GeneratedPostRepository;
import com.linkedinagent.repository.LearningPatternRepository;
import com.linkedinagent.repository.ResearchDataRepository;
import com.linkedinagent.repository.TopicRepository;
import com.linkedinagent.service.AgentLogService;
import com.linkedinagent.util.GeminiRateLimiter;
import com.linkedinagent.util.PostContentAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentCreationAgent {

    public static final String AGENT_NAME = "ContentCreationAgent";
    private static final int MAX_LEARNING_PATTERNS = 5;
    private static final int MIN_WORDS = 150;
    private static final int MAX_WORDS = 300;

    private final ResearchDataRepository researchDataRepository;
    private final TopicRepository topicRepository;
    private final LearningPatternRepository learningPatternRepository;
    private final GeneratedPostRepository generatedPostRepository;
    private final AgentLogService agentLogService;
    private final GeminiRateLimiter geminiRateLimiter;
    private final ObjectMapper objectMapper;

    @Transactional
    public ContentCreationOutput run(UUID userId, UUID runId, UUID researchId) {
        long startMs = System.currentTimeMillis();
        UUID logId = agentLogService.startLog(userId, AGENT_NAME, runId,
                "Creating post from research " + researchId).getId();

        try {
            ResearchData research = researchDataRepository.findByIdAndUserId(researchId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Research data not found"));

            if (generatedPostRepository.existsByResearchId(researchId)) {
                throw new AgentException("Post already exists for research " + researchId);
            }

            Topic topic = topicRepository.findById(research.getTopicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

            List<LearningPattern> patterns = learningPatternRepository
                    .findRecentByUserId(userId, PageRequest.of(0, MAX_LEARNING_PATTERNS));

            GeminiPostContent geminiContent = generateContent(research, topic, patterns);
            PostContent content = geminiContent.toPostContent();
            int wordCount = PostContentAssembler.countPostWords(content);

            if (wordCount < MIN_WORDS || wordCount > MAX_WORDS) {
                log.warn("Generated post word count {} outside target range {}-{}", wordCount, MIN_WORDS, MAX_WORDS);
            }

            String fullText = PostContentAssembler.assembleFullText(content);
            String title = deriveTitle(geminiContent);

            GeneratedPost post = GeneratedPost.builder()
                    .userId(userId)
                    .topicId(topic.getId())
                    .researchId(research.getId())
                    .title(title)
                    .hook(content.hook())
                    .body(content.body())
                    .keyTakeaways(content.keyTakeaways())
                    .callToAction(content.callToAction())
                    .fullText(fullText)
                    .wordCount(wordCount)
                    .status(PostStatus.draft)
                    .build();

            try {
                post = generatedPostRepository.save(post);
            } catch (Exception e) {
                log.error("Failed to save generated post for user={}, research={}", userId, researchId, e);
                throw new AgentException("Failed to save generated post", e);
            }

            agentLogService.completeLog(logId, AgentStatus.success,
                    "Created post " + post.getId() + " (" + wordCount + " words)",
                    null, System.currentTimeMillis() - startMs);

            log.info("ContentCreationAgent completed for user={}, postId={}", userId, post.getId());

            return new ContentCreationOutput(
                    post.getId(),
                    research.getId(),
                    topic.getId(),
                    content,
                    fullText,
                    wordCount
            );
        } catch (Exception e) {
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);
            if (e instanceof AgentException || e instanceof ResourceNotFoundException) {
                throw e;
            }
            throw new AgentException("Content creation failed", e);
        }
    }

    @Transactional
    public List<ContentCreationOutput> runForUnprocessedResearch(UUID userId, UUID runId) {
        List<ResearchData> unprocessed = researchDataRepository.findUnprocessedByUserId(userId);
        if (unprocessed.isEmpty()) {
            log.info("No unprocessed research found for user={}", userId);
            return List.of();
        }

        return unprocessed.stream()
                .limit(3)
                .map(research -> run(userId, runId, research.getId()))
                .toList();
    }

    private GeminiPostContent generateContent(ResearchData research, Topic topic, List<LearningPattern> patterns) {
        String promptText = buildPrompt(research, topic, patterns);

        String geminiResponse = geminiRateLimiter.call(new Prompt(promptText))
                .getResult()
                .getOutput()
                .getText();

        return parsePostContent(geminiResponse);
    }

    private String buildPrompt(ResearchData research, Topic topic, List<LearningPattern> patterns) {
        String patternContext = formatPatterns(patterns);
        String concepts = research.getKeyConcepts() != null
                ? String.join(", ", research.getKeyConcepts())
                : "none";

        return """
                You are an expert LinkedIn content creator for tech professionals.
                Write a LinkedIn post based on the research below.

                TOPIC: %s (category: %s)
                RESEARCH SUMMARY: %s
                KEY CONCEPTS: %s
                SOURCE: %s

                LEARNING PATTERNS FROM PAST POSTS (apply what worked, avoid what failed):
                %s

                CONSTRAINTS:
                - Total body + hook: 150-300 words
                - Tone: professional yet conversational
                - Use 2-4 relevant emojis (not more)
                - Hook must create curiosity and stop the scroll
                - Include exactly 3 key takeaways
                - Include 3-5 relevant hashtags (without # prefix in JSON)
                - Call to action should invite engagement (comment, share opinion)

                Return ONLY valid JSON (no markdown fences):
                {
                  "title": "short post title for internal use",
                  "hook": "attention-grabbing opening line",
                  "body": "main post body",
                  "keyTakeaways": ["takeaway 1", "takeaway 2", "takeaway 3"],
                  "callToAction": "engagement question or CTA",
                  "hashtags": ["LinkedIn", "Tech", "AI"]
                }
                """.formatted(
                topic.getName(),
                topic.getCategory(),
                research.getSummary(),
                concepts,
                research.getSourceUrl() != null ? research.getSourceUrl() : "N/A",
                patternContext
        );
    }

    private String formatPatterns(List<LearningPattern> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return "No past patterns available yet. Use best practices for tech LinkedIn posts.";
        }

        return patterns.stream()
                .map(p -> "- [%s] category=%s engagement=%.2f%% insight: %s".formatted(
                        p.getPatternType().name(),
                        p.getTopicCategory() != null ? p.getTopicCategory() : "general",
                        p.getAvgEngagementRate() * 100,
                        p.getInsight() != null ? p.getInsight() : "n/a"))
                .collect(Collectors.joining("\n"));
    }

    private GeminiPostContent parsePostContent(String response) {
        try {
            String json = extractJson(response);
            GeminiPostContent parsed = objectMapper.readValue(json, GeminiPostContent.class);
            validatePostContent(parsed);
            return parsed;
        } catch (AgentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini post content: {}", response, e);
            throw new AgentException("Failed to parse Gemini post content");
        }
    }

    private void validatePostContent(GeminiPostContent content) {
        if (content.hook() == null || content.hook().isBlank()) {
            throw new AgentException("Gemini returned empty hook");
        }
        if (content.body() == null || content.body().isBlank()) {
            throw new AgentException("Gemini returned empty body");
        }
        if (content.keyTakeaways() == null || content.keyTakeaways().size() < 3) {
            throw new AgentException("Gemini must return at least 3 key takeaways");
        }
        if (content.callToAction() == null || content.callToAction().isBlank()) {
            throw new AgentException("Gemini returned empty call to action");
        }
        if (content.hashtags() == null || content.hashtags().isEmpty()) {
            throw new AgentException("Gemini returned no hashtags");
        }
    }

    private String deriveTitle(GeminiPostContent content) {
        if (content.title() != null && !content.title().isBlank()) {
            return truncate(content.title(), 500);
        }
        return truncate(content.hook(), 500);
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new AgentException("Gemini response did not contain JSON");
        }
        return response.substring(start, end + 1);
    }
}
