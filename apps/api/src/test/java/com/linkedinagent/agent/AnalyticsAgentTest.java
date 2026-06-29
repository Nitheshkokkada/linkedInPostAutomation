package com.linkedinagent.agent;

import com.linkedinagent.entity.Analytics;
import com.linkedinagent.entity.PublishedPost;
import com.linkedinagent.entity.User;
import com.linkedinagent.entity.enums.PostingMode;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.AnalyticsRepository;
import com.linkedinagent.repository.PublishedPostRepository;
import com.linkedinagent.repository.UserRepository;
import com.linkedinagent.service.AgentLogService;
import com.linkedinagent.service.LinkedInOAuthService;
import com.linkedinagent.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsAgentTest {

    @Mock
    private PublishedPostRepository publishedPostRepository;
    @Mock
    private AnalyticsRepository analyticsRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LinkedInOAuthService linkedInOAuthService;
    @Mock
    private AgentLogService agentLogService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AnalyticsAgent analyticsAgent;

    private UUID userId;
    private UUID publishedPostId;
    private PublishedPost publishedPost;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        publishedPostId = UUID.randomUUID();
        publishedPost = PublishedPost.builder()
                .id(publishedPostId)
                .userId(userId)
                .linkedinPostId("12345678")
                .linkedinPostUrl("https://linkedin.com/feed/update/12345678")
                .publishedAt(OffsetDateTime.now().minusHours(25))
                .build();
        user = User.builder()
                .id(userId)
                .email("test@test.com")
                .fullName("Test User")
                .linkedinAccessToken("encrypted-token")
                .linkedinProfileId("abc123")
                .postingMode(PostingMode.auto)
                .build();
    }

    @Test
    void fetchForPublishedPost_shouldSaveAnalyticsWhenTokenValid() {
        when(publishedPostRepository.findById(publishedPostId)).thenReturn(Optional.of(publishedPost));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(linkedInOAuthService.getDecryptedAccessToken(userId)).thenReturn("valid-token");
        when(analyticsRepository.findTopByPublishedPostIdOrderByFetchedAtDesc(publishedPostId))
                .thenReturn(Optional.empty());
        when(analyticsRepository.save(any(Analytics.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock RestTemplate to return empty stats (LinkedIn API would return real data)
        org.springframework.http.ResponseEntity<com.linkedinagent.domain.LinkedInShareStats> response =
                org.springframework.http.ResponseEntity.ok(new com.linkedinagent.domain.LinkedInShareStats(0, 0, 0, 0));
        when(restTemplate.exchange(
                any(String.class),
                eq(org.springframework.http.HttpMethod.GET),
                any(),
                eq(com.linkedinagent.domain.LinkedInShareStats.class)))
                .thenReturn(response);

        analyticsAgent.fetchForPublishedPost(publishedPostId, "24h");

        verify(analyticsRepository, times(1)).save(any(Analytics.class));
    }

    @Test
    void fetchForPublishedPost_shouldThrowWhenPostNotFound() {
        when(publishedPostRepository.findById(publishedPostId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsAgent.fetchForPublishedPost(publishedPostId, "24h"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
