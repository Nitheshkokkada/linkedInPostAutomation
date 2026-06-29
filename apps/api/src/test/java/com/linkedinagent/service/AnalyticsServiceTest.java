package com.linkedinagent.service;

import com.linkedinagent.dto.response.AnalyticsResponse;
import com.linkedinagent.entity.Analytics;
import com.linkedinagent.entity.PublishedPost;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.AnalyticsRepository;
import com.linkedinagent.repository.PublishedPostRepository;
import com.linkedinagent.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository analyticsRepository;
    @Mock
    private PublishedPostRepository publishedPostRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private UUID userId;
    private UUID publishedPostId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        publishedPostId = UUID.randomUUID();
    }

    @Test
    void getAnalyticsForPost_shouldReturnAnalytics() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            PublishedPost post = PublishedPost.builder()
                    .id(publishedPostId)
                    .userId(userId)
                    .build();
            when(publishedPostRepository.findById(publishedPostId)).thenReturn(Optional.of(post));

            Analytics analytics = Analytics.builder()
                    .id(UUID.randomUUID())
                    .publishedPostId(publishedPostId)
                    .userId(userId)
                    .impressions(100)
                    .likes(5)
                    .comments(2)
                    .shares(1)
                    .engagementRate(8.0f)
                    .fetchedAt(OffsetDateTime.now())
                    .build();
            when(analyticsRepository.findByPublishedPostIdOrderByFetchedAtDesc(publishedPostId))
                    .thenReturn(List.of(analytics));

            List<AnalyticsResponse> result = analyticsService.getAnalyticsForPost(publishedPostId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).impressions()).isEqualTo(100);
        }
    }

    @Test
    void getPublishedCount_shouldReturnCount() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(publishedPostRepository.countByUserId(userId)).thenReturn(5L);

            long count = analyticsService.getPublishedCount();

            assertThat(count).isEqualTo(5);
        }
    }
}
