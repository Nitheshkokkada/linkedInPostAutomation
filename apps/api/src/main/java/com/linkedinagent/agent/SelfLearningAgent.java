package com.linkedinagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedinagent.domain.GeminiPatternAnalysis;
import com.linkedinagent.entity.Analytics;
import com.linkedinagent.entity.GeneratedPost;
import com.linkedinagent.entity.LearningPattern;
import com.linkedinagent.entity.enums.PatternType;
import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.exception.AgentException;
import com.linkedinagent.repository.AnalyticsRepository;
import com.linkedinagent.repository.GeneratedPostRepository;
import com.linkedinagent.repository.LearningPatternRepository;
import com.linkedinagent.repository.UserRepository;
import com.linkedinagent.service.AgentLogService;
import com.linkedinagent.util.GeminiRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelfLearningAgent {

    public static final String AGENT_NAME = "SelfLearningAgent";
    private static final int MIN_POSTS_FOR_ANALYSIS = 3;

    private final GeneratedPostRepository generatedPostRepository;
    private final AnalyticsRepository analyticsRepository;
    private final LearningPatternRepository learningPatternRepository;
    private final AgentLogService agentLogService;
    private final GeminiRateLimiter geminiRateLimiter;
    private final ObjectMapper objectMapper;

    @Transactional
    public void runForAllUsers() {
        long startMs = System.currentTimeMillis();
        UUID runId = UUID.randomUUID();
        UUID logId = agentLogService.startLog(null, AGENT_NAME, runId,
                "Self-learning pattern analysis for all users").getId();

        try {
            List<UUID> userIds = generatedPostRepository.findAll().stream()
                    .map(GeneratedPost::getUserId)
                    .distinct()
                    .toList();

            for (UUID userId : userIds) {
                try {
                    analyzePatterns(userId);
                } catch (Exception e) {
                    log.warn("Pattern analysis failed for userId={}: {}", userId, e.getMessage());
                }
            }

            agentLogService.completeLog(logId, AgentStatus.success,
                    "Pattern analysis completed for " + userIds.size() + " users",
                    null, System.currentTimeMillis() - startMs);
        } catch (Exception e) {
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);
            throw new AgentException("Self-learning analysis failed", e);
        }
    }

    @Transactional
    public void analyzePatterns(UUID userId) {
        long startMs = System.currentTimeMillis();
        UUID runId = UUID.randomUUID();
        UUID logId = agentLogService.startLog(userId, AGENT_NAME, runId,
                "Pattern analysis for user " + userId).getId();

        try {
            List<GeneratedPost> posts = generatedPostRepository.findByUserIdAndStatusIn(
                    userId, List.of(PostStatus.published, PostStatus.approved));

            if (posts.size() < MIN_POSTS_FOR_ANALYSIS) {
                agentLogService.completeLog(logId, AgentStatus.success,
                        "Not enough posts for analysis (" + posts.size() + "/" + MIN_POSTS_FOR_ANALYSIS + ")",
                        null, System.currentTimeMillis() - startMs);
                return;
            }

            List<Map<String, Object>> postAnalyticsData = new ArrayList<>();
            for (GeneratedPost post : posts) {
                Optional<Analytics> latestAnalytics = analyticsRepository
                        .findTopByPublishedPostIdOrderByFetchedAtDesc(post.getId());
                if (latestAnalytics.isPresent()) {
                    Analytics a = latestAnalytics.get();
                    postAnalyticsData.add(Map.of(
                            "title", post.getTitle() != null ? post.getTitle() : "",
                            "topic_category", post.getTopicId() != null ? resolveTopicCategory(post) : "unknown",
                            "word_count", post.getWordCount() != null ? post.getWordCount() : 0,
                            "quality_score", post.getQualityScore() != null ? post.getQualityScore() : 0,
                            "engagement_rate", a.getEngagementRate(),
                            "impressions", a.getImpressions(),
                            "likes", a.getLikes(),
                            "comments", a.getComments()
                    ));
                }
            }

            if (postAnalyticsData.size() < MIN_POSTS_FOR_ANALYSIS) {
                agentLogService.completeLog(logId, AgentStatus.success,
                        "Not enough posts with analytics for pattern analysis",
                        null, System.currentTimeMillis() - startMs);
                return;
            }

            GeminiPatternAnalysis analysis = generatePatternInsight(postAnalyticsData);

            upsertPatterns(userId, analysis);

            agentLogService.completeLog(logId, AgentStatus.success,
                    "Found " + analysis.patterns().size() + " patterns",
                    null, System.currentTimeMillis() - startMs);
        } catch (Exception e) {
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);
            throw new AgentException("Pattern analysis failed for user " + userId, e);
        }
    }

    private String resolveTopicCategory(GeneratedPost post) {
        return "unknown";
    }

    private GeminiPatternAnalysis generatePatternInsight(List<Map<String, Object>> postData) {
        String postDataJson;
        try {
            postDataJson = objectMapper.writeValueAsString(postData);
        } catch (Exception e) {
            throw new AgentException("Failed to serialize post data for analysis", e);
        }

        String promptText = """
                You are a LinkedIn content performance analyst. Analyze the following post data and identify patterns
                that correlate with high and low engagement.

                Return ONLY valid JSON with this exact structure (no markdown):
                {
                  "patterns": [
                    {
                      "type": "success or failure",
                      "topic_category": "ai|software_engineering|java|spring_boot|cloud|system_design|career",
                      "content_features": {"feature_key": "value"},
                      "avg_engagement_rate": 0.0,
                      "sample_size": 1,
                      "insight": "Brief description of the pattern"
                    }
                  ]
                }

                Post data:
                %s
                """.formatted(postDataJson);

        String geminiResponse = geminiRateLimiter.call(new Prompt(promptText))
                .getResult()
                .getOutput()
                .getText();

        return parseAnalysis(geminiResponse);
    }

    private GeminiPatternAnalysis parseAnalysis(String response) {
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, GeminiPatternAnalysis.class);
        } catch (Exception e) {
            log.error("Failed to parse Gemini pattern analysis response: {}", response, e);
            throw new AgentException("Failed to parse Gemini pattern analysis");
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new AgentException("Gemini response did not contain JSON");
        }
        return response.substring(start, end + 1);
    }

    private void upsertPatterns(UUID userId, GeminiPatternAnalysis analysis) {
        List<LearningPattern> patterns = new ArrayList<>();

        for (GeminiPatternAnalysis.PatternData patternData : analysis.patterns()) {
            PatternType type = "success".equals(patternData.type()) ? PatternType.success : PatternType.failure;

            Optional<LearningPattern> existing = learningPatternRepository
                    .findByUserIdAndTopicCategory(userId, patternData.topicCategory())
                    .stream()
                    .filter(lp -> lp.getPatternType() == type)
                    .findFirst();

            if (existing.isPresent()) {
                LearningPattern lp = existing.get();
                lp.setContentFeatures(patternData.contentFeatures());
                lp.setAvgEngagementRate(patternData.avgEngagementRate());
                lp.setSampleSize(patternData.sampleSize());
                lp.setInsight(patternData.insight());
                patterns.add(lp);
            } else {
                patterns.add(LearningPattern.builder()
                        .userId(userId)
                        .patternType(type)
                        .topicCategory(patternData.topicCategory())
                        .contentFeatures(patternData.contentFeatures())
                        .avgEngagementRate(patternData.avgEngagementRate())
                        .sampleSize(patternData.sampleSize())
                        .insight(patternData.insight())
                        .build());
            }
        }

        if (!patterns.isEmpty()) {
            learningPatternRepository.saveAll(patterns);
            log.info("Saved {} learning patterns for user={}", patterns.size(), userId);
        }
    }
}
