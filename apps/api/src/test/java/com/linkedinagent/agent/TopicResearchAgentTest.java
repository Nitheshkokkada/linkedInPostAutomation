package com.linkedinagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedinagent.domain.GeminiResearchAnalysis;
import com.linkedinagent.domain.ResearchOutput;
import com.linkedinagent.domain.TavilySearchResponse;
import com.linkedinagent.entity.AgentLog;
import com.linkedinagent.entity.Topic;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.entity.enums.TopicCategory;
import com.linkedinagent.repository.ResearchDataRepository;
import com.linkedinagent.repository.TopicRepository;
import com.linkedinagent.service.AgentLogService;
import com.linkedinagent.util.GeminiRateLimiter;
import com.linkedinagent.util.TavilySearchClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicResearchAgentTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private ResearchDataRepository researchDataRepository;

    @Mock
    private AgentLogService agentLogService;

    @Mock
    private TavilySearchClient tavilySearchClient;

    @Mock
    private GeminiRateLimiter geminiRateLimiter;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TopicResearchAgent topicResearchAgent;

    @Test
    void runResearchesActiveTopicsAndSavesResults() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID logId = UUID.randomUUID();

        Topic topic = Topic.builder()
                .id(topicId)
                .userId(userId)
                .name("Spring Boot 3")
                .category(TopicCategory.spring_boot)
                .isActive(true)
                .priority(1)
                .build();

        TavilySearchResponse.TavilyResult tavilyResult =
                new TavilySearchResponse.TavilyResult("Spring Boot News", "https://example.com", "Content snippet");
        TavilySearchResponse tavilyResponse = new TavilySearchResponse(List.of(tavilyResult));

        when(agentLogService.startLog(any(), anyString(), any(), anyString()))
                .thenReturn(AgentLog.builder().id(logId).build());
        when(topicRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(userId)).thenReturn(List.of(topic));
        when(tavilySearchClient.search(anyString())).thenReturn(tavilyResponse);
        when(tavilySearchClient.topResult(tavilyResponse)).thenReturn(tavilyResult);
        when(tavilySearchClient.formatResultsForPrompt(tavilyResponse)).thenReturn("formatted results");

        Generation generation = new Generation(new AssistantMessage("""
                {"summary":"Great summary","keyConcepts":["a","b","c"],"relevanceScore":0.9}
                """));
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        when(geminiRateLimiter.call(any(Prompt.class))).thenReturn(chatResponse);
        when(objectMapper.readValue(anyString(), eq(GeminiResearchAnalysis.class)))
                .thenReturn(new GeminiResearchAnalysis("Great summary", List.of("a", "b", "c"), 0.9f));

        List<ResearchOutput> outputs = topicResearchAgent.run(userId, runId);

        assertThat(outputs).hasSize(1);
        assertThat(outputs.getFirst().topicId()).isEqualTo(topicId);
        assertThat(outputs.getFirst().sourceUrl()).isEqualTo("https://example.com");

        verify(researchDataRepository).saveAll(any());
        verify(agentLogService).completeLog(eq(logId), eq(AgentStatus.success), anyString(), eq(null), anyLong());
    }
}
