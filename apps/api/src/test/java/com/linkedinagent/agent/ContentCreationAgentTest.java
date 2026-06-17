package com.linkedinagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedinagent.domain.ContentCreationOutput;
import com.linkedinagent.domain.GeminiPostContent;
import com.linkedinagent.entity.AgentLog;
import com.linkedinagent.entity.GeneratedPost;
import com.linkedinagent.entity.ResearchData;
import com.linkedinagent.entity.Topic;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.entity.enums.TopicCategory;
import com.linkedinagent.repository.GeneratedPostRepository;
import com.linkedinagent.repository.LearningPatternRepository;
import com.linkedinagent.repository.ResearchDataRepository;
import com.linkedinagent.repository.TopicRepository;
import com.linkedinagent.service.AgentLogService;
import com.linkedinagent.util.GeminiRateLimiter;
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
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentCreationAgentTest {

    @Mock
    private ResearchDataRepository researchDataRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private LearningPatternRepository learningPatternRepository;

    @Mock
    private GeneratedPostRepository generatedPostRepository;

    @Mock
    private AgentLogService agentLogService;

    @Mock
    private GeminiRateLimiter geminiRateLimiter;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ContentCreationAgent contentCreationAgent;

    @Test
    void runCreatesPostFromResearch() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID researchId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID logId = UUID.randomUUID();

        ResearchData research = ResearchData.builder()
                .id(researchId)
                .topicId(topicId)
                .summary("AI agents are transforming software development.")
                .keyConcepts(List.of("agents", "automation", "LLMs"))
                .sourceUrl("https://example.com/article")
                .build();

        Topic topic = Topic.builder()
                .id(topicId)
                .userId(userId)
                .name("AI Agents")
                .category(TopicCategory.ai)
                .build();

        GeminiPostContent geminiContent = new GeminiPostContent(
                "AI Agents in 2026",
                "Are AI agents the next big shift?",
                "Teams are shipping faster with agentic workflows and smarter tooling.",
                List.of("Agents automate research", "Human review stays essential", "Start small, iterate fast"),
                "How is your team using AI agents?",
                List.of("AI", "SoftwareEngineering", "Innovation")
        );

        when(agentLogService.startLog(any(), anyString(), any(), anyString()))
                .thenReturn(AgentLog.builder().id(logId).build());
        when(researchDataRepository.findByIdAndUserId(researchId, userId)).thenReturn(Optional.of(research));
        when(generatedPostRepository.existsByResearchId(researchId)).thenReturn(false);
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(learningPatternRepository.findRecentByUserId(eq(userId), any(Pageable.class))).thenReturn(List.of());

        Generation generation = new Generation(new AssistantMessage("""
                {"title":"AI Agents","hook":"Hook","body":"Body","keyTakeaways":["a","b","c"],"callToAction":"CTA?","hashtags":["AI"]}
                """));
        when(geminiRateLimiter.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(generation)));
        when(objectMapper.readValue(anyString(), eq(GeminiPostContent.class))).thenReturn(geminiContent);

        GeneratedPost savedPost = GeneratedPost.builder().id(postId).build();
        when(generatedPostRepository.save(any(GeneratedPost.class))).thenReturn(savedPost);

        ContentCreationOutput output = contentCreationAgent.run(userId, runId, researchId);

        assertThat(output.postId()).isEqualTo(postId);
        assertThat(output.researchId()).isEqualTo(researchId);
        assertThat(output.topicId()).isEqualTo(topicId);
        assertThat(output.content().hook()).isEqualTo("Are AI agents the next big shift?");
        assertThat(output.fullText()).contains("✅ Key Takeaways:");

        ArgumentCaptor<GeneratedPost> postCaptor = ArgumentCaptor.forClass(GeneratedPost.class);
        verify(generatedPostRepository).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getTitle()).isEqualTo("AI Agents in 2026");
        assertThat(postCaptor.getValue().getUserId()).isEqualTo(userId);

        verify(agentLogService).completeLog(eq(logId), eq(AgentStatus.success), anyString(), eq(null), anyLong());
        verify(geminiRateLimiter).call(any(Prompt.class));
    }
}
