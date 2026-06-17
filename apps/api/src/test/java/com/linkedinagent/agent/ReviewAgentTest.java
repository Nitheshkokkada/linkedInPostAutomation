package com.linkedinagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedinagent.domain.GeminiReviewAnalysis;
import com.linkedinagent.domain.ReviewOutput;
import com.linkedinagent.entity.AgentLog;
import com.linkedinagent.entity.GeneratedPost;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.repository.GeneratedPostRepository;
import com.linkedinagent.repository.ResearchDataRepository;
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
class ReviewAgentTest {

    @Mock
    private GeneratedPostRepository generatedPostRepository;

    @Mock
    private ResearchDataRepository researchDataRepository;

    @Mock
    private AgentLogService agentLogService;

    @Mock
    private GeminiRateLimiter geminiRateLimiter;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ReviewAgent reviewAgent;

    @Test
    void runApprovesHighScoringPost() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID logId = UUID.randomUUID();

        String postText = """
                AI agents help software teams move faster every week. They automate research tasks that used to take hours.
                Writers use agents to draft posts and refine them quickly. Engineers use agents to explore new frameworks.
                The best teams keep humans in the review loop at all times. This balance protects quality and brand voice.
                What is your team doing with AI agents this quarter?
                """.trim();

        GeneratedPost post = GeneratedPost.builder()
                .id(postId)
                .userId(userId)
                .fullText(postText)
                .status(PostStatus.draft)
                .build();

        GeminiReviewAnalysis analysis = new GeminiReviewAnalysis(25, 25, "Strong post", "");

        when(agentLogService.startLog(any(), anyString(), any(), anyString()))
                .thenReturn(AgentLog.builder().id(logId).build());
        when(generatedPostRepository.findByIdAndUserId(postId, userId)).thenReturn(Optional.of(post));
        when(researchDataRepository.findById(any())).thenReturn(Optional.empty());

        Generation generation = new Generation(new AssistantMessage(
                "{\"grammarClarity\":23,\"technicalAccuracy\":22,\"feedback\":\"Strong post\",\"rejectionReason\":\"\"}"));
        when(geminiRateLimiter.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(generation)));
        when(objectMapper.readValue(anyString(), eq(GeminiReviewAnalysis.class))).thenReturn(analysis);
        when(generatedPostRepository.findRecentByUserId(eq(userId), any(Pageable.class))).thenReturn(List.of());
        when(geminiRateLimiter.embedBatch(any())).thenReturn(List.of(new float[]{1f}));
        when(generatedPostRepository.save(any(GeneratedPost.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewOutput output = reviewAgent.run(userId, runId, postId);

        assertThat(output.approved()).isTrue();
        assertThat(output.qualityScore()).isGreaterThanOrEqualTo(85);
        assertThat(output.status()).isEqualTo(PostStatus.approved);

        ArgumentCaptor<GeneratedPost> captor = ArgumentCaptor.forClass(GeneratedPost.class);
        verify(generatedPostRepository).save(captor.capture());
        assertThat(captor.getValue().getQualityScore()).isGreaterThanOrEqualTo(85);
        assertThat(captor.getValue().getQualityFeedback()).containsKeys("grammar_clarity", "readability");

        verify(agentLogService).completeLog(eq(logId), eq(AgentStatus.success), anyString(), eq(null), anyLong());
    }

    @Test
    void runRejectsLowScoringPost() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID logId = UUID.randomUUID();

        GeneratedPost post = GeneratedPost.builder()
                .id(postId)
                .userId(userId)
                .fullText("Bad post.")
                .status(PostStatus.draft)
                .build();

        GeminiReviewAnalysis analysis = new GeminiReviewAnalysis(10, 10, "Needs work", "Poor quality");

        when(agentLogService.startLog(any(), anyString(), any(), anyString()))
                .thenReturn(AgentLog.builder().id(logId).build());
        when(generatedPostRepository.findByIdAndUserId(postId, userId)).thenReturn(Optional.of(post));
        when(researchDataRepository.findById(any())).thenReturn(Optional.empty());

        Generation generation = new Generation(new AssistantMessage(
                "{\"grammarClarity\":10,\"technicalAccuracy\":10,\"feedback\":\"Needs work\",\"rejectionReason\":\"Poor quality\"}"));
        when(geminiRateLimiter.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(generation)));
        when(objectMapper.readValue(anyString(), eq(GeminiReviewAnalysis.class))).thenReturn(analysis);
        when(generatedPostRepository.findRecentByUserId(eq(userId), any(Pageable.class))).thenReturn(List.of());
        when(geminiRateLimiter.embedBatch(any())).thenReturn(List.of(new float[]{1f}));
        when(generatedPostRepository.save(any(GeneratedPost.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewOutput output = reviewAgent.run(userId, runId, postId);

        assertThat(output.approved()).isFalse();
        assertThat(output.status()).isEqualTo(PostStatus.rejected);
        assertThat(output.rejectionReason()).isNotBlank();
    }
}
