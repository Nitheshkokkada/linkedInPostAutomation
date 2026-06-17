package com.linkedinagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedinagent.domain.GeminiResearchAnalysis;
import com.linkedinagent.domain.ResearchOutput;
import com.linkedinagent.domain.TavilySearchResponse;
import com.linkedinagent.entity.ResearchData;
import com.linkedinagent.entity.Topic;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.exception.AgentException;
import com.linkedinagent.repository.ResearchDataRepository;
import com.linkedinagent.repository.TopicRepository;
import com.linkedinagent.service.AgentLogService;
import com.linkedinagent.util.GeminiRateLimiter;
import com.linkedinagent.util.TavilySearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicResearchAgent {

    public static final String AGENT_NAME = "TopicResearchAgent";
    private static final int MAX_TOPICS_PER_RUN = 3;

    private final TopicRepository topicRepository;
    private final ResearchDataRepository researchDataRepository;
    private final AgentLogService agentLogService;
    private final TavilySearchClient tavilySearchClient;
    private final GeminiRateLimiter geminiRateLimiter;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<ResearchOutput> run(UUID userId, UUID runId) {
        long startMs = System.currentTimeMillis();
        UUID logId = agentLogService.startLog(userId, AGENT_NAME, runId,
                "Researching up to " + MAX_TOPICS_PER_RUN + " active topics").getId();

        try {
            List<Topic> topics = topicRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(userId)
                    .stream()
                    .limit(MAX_TOPICS_PER_RUN)
                    .toList();

            if (topics.isEmpty()) {
                agentLogService.completeLog(logId, AgentStatus.success,
                        "No active topics found", null, System.currentTimeMillis() - startMs);
                return List.of();
            }

            List<ResearchOutput> outputs = new ArrayList<>();
            List<ResearchData> toSave = new ArrayList<>();

            for (Topic topic : topics) {
                ResearchOutput output = researchTopic(topic);
                outputs.add(output);
                toSave.add(ResearchData.builder()
                        .topicId(output.topicId())
                        .sourceUrl(output.sourceUrl())
                        .sourceTitle(topic.getName())
                        .summary(output.summary())
                        .keyConcepts(output.keyConcepts())
                        .relevanceScore(output.relevanceScore())
                        .build());
            }

            researchDataRepository.saveAll(toSave);

            agentLogService.completeLog(logId, AgentStatus.success,
                    "Researched " + outputs.size() + " topics", null, System.currentTimeMillis() - startMs);

            log.info("TopicResearchAgent completed for user={}, topics={}", userId, outputs.size());
            return outputs;
        } catch (Exception e) {
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);
            if (e instanceof AgentException) {
                throw e;
            }
            throw new AgentException("Topic research failed", e);
        }
    }

    private ResearchOutput researchTopic(Topic topic) {
        TavilySearchResponse searchResponse = tavilySearchClient.search(
                topic.getName() + " " + topic.getCategory().name() + " latest trends");

        TavilySearchResponse.TavilyResult topResult = tavilySearchClient.topResult(searchResponse);
        String searchContext = tavilySearchClient.formatResultsForPrompt(searchResponse);

        String promptText = """
                You are a research analyst. Analyze the following search results for the topic "%s" (category: %s).
                Return ONLY valid JSON with this exact structure (no markdown):
                {"summary":"2-3 sentence summary","keyConcepts":["concept1","concept2","concept3"],"relevanceScore":0.85}
                relevanceScore is 0.0-1.0 indicating how relevant the content is for a LinkedIn tech audience.

                Search results:
                %s
                """.formatted(topic.getName(), topic.getCategory(), searchContext);

        String geminiResponse = geminiRateLimiter.call(new Prompt(promptText))
                .getResult()
                .getOutput()
                .getText();

        GeminiResearchAnalysis analysis = parseAnalysis(geminiResponse);

        return new ResearchOutput(
                topic.getId(),
                topResult.url(),
                analysis.keyConcepts(),
                analysis.summary(),
                analysis.relevanceScore()
        );
    }

    private GeminiResearchAnalysis parseAnalysis(String response) {
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, GeminiResearchAnalysis.class);
        } catch (Exception e) {
            log.error("Failed to parse Gemini research response: {}", response, e);
            throw new AgentException("Failed to parse Gemini research analysis");
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
}
