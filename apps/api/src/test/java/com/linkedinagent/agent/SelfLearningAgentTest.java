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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelfLearningAgentTest {

    @Mock
    private GeneratedPostRepository generatedPostRepository;
    @Mock
    private AnalyticsRepository analyticsRepository;
    @Mock
    private LearningPatternRepository learningPatternRepository;
    @Mock
    private AgentLogService agentLogService;
    @Mock
    private GeminiRateLimiter geminiRateLimiter;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SelfLearningAgent selfLearningAgent;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void analyzePatterns_shouldSkipWhenNotEnoughPosts() {
        when(generatedPostRepository.findByUserIdAndStatusIn(
                eq(userId), any())).thenReturn(List.of());

        selfLearningAgent.analyzePatterns(userId);

        verify(geminiRateLimiter, never()).call(any(Prompt.class));
    }

    @Test
    void analyzePatterns_shouldAnalyzeWhenEnoughPosts() {
        UUID postId1 = UUID.randomUUID();
        UUID postId2 = UUID.randomUUID();
        UUID postId3 = UUID.randomUUID();

        GeneratedPost post1 = GeneratedPost.builder().id(postId1).userId(userId).title("Post 1").status(PostStatus.published).wordCount(200).build();
        GeneratedPost post2 = GeneratedPost.builder().id(postId2).userId(userId).title("Post 2").status(PostStatus.published).wordCount(150).build();
        GeneratedPost post3 = GeneratedPost.builder().id(postId3).userId(userId).title("Post 3").status(PostStatus.approved).wordCount(180).build();

        when(generatedPostRepository.findByUserIdAndStatusIn(
                eq(userId), any())).thenReturn(List.of(post1, post2, post3));

        Analytics analytics1 = Analytics.builder().id(UUID.randomUUID()).publishedPostId(postId1).userId(userId).impressions(100).likes(10).comments(5).shares(2).engagementRate(17.0f).fetchedAt(OffsetDateTime.now()).build();
        Analytics analytics2 = Analytics.builder().id(UUID.randomUUID()).publishedPostId(postId2).userId(userId).impressions(50).likes(3).comments(1).shares(0).engagementRate(8.0f).fetchedAt(OffsetDateTime.now()).build();
        Analytics analytics3 = Analytics.builder().id(UUID.randomUUID()).publishedPostId(postId3).userId(userId).impressions(200).likes(25).comments(10).shares(5).engagementRate(20.0f).fetchedAt(OffsetDateTime.now()).build();

        when(analyticsRepository.findTopByPublishedPostIdOrderByFetchedAtDesc(postId1))
                .thenReturn(Optional.of(analytics1));
        when(analyticsRepository.findTopByPublishedPostIdOrderByFetchedAtDesc(postId2))
                .thenReturn(Optional.of(analytics2));
        when(analyticsRepository.findTopByPublishedPostIdOrderByFetchedAtDesc(postId3))
                .thenReturn(Optional.of(analytics3));

        String geminiResponse = """
                {"patterns":[{"type":"success","topic_category":"ai","content_features":{"word_count":"high"},"avg_engagement_rate":18.5,"sample_size":3,"insight":"Longer posts perform better"}]}
                """;

        Generation generation = new Generation(new AssistantMessage(geminiResponse));
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        when(geminiRateLimiter.call(any(Prompt.class))).thenReturn(chatResponse);

        when(learningPatternRepository.findByUserIdAndTopicCategory(userId, "ai"))
                .thenReturn(Optional.empty());
        when(learningPatternRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        selfLearningAgent.analyzePatterns(userId);

        verify(geminiRateLimiter, times(1)).call(any(Prompt.class));
        verify(learningPatternRepository, times(1)).saveAll(any());
    }
}
