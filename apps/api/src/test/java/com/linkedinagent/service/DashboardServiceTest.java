package com.linkedinagent.service;

import com.linkedinagent.dto.response.DashboardSummaryResponse;
import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.entity.enums.ScheduledPostStatus;
import com.linkedinagent.repository.AnalyticsRepository;
import com.linkedinagent.repository.GeneratedPostRepository;
import com.linkedinagent.repository.ScheduledPostRepository;
import com.linkedinagent.repository.TopicRepository;
import com.linkedinagent.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private GeneratedPostRepository generatedPostRepository;
    @Mock private ScheduledPostRepository scheduledPostRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private AnalyticsRepository analyticsRepository;
    @Mock private GeminiUsageService geminiUsageService;

    @InjectMocks
    private DashboardService dashboardService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void getSummary_shouldReturnDashboardData() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            when(generatedPostRepository.countByUserId(userId)).thenReturn(10L);
            when(generatedPostRepository.countByUserIdAndStatus(userId, PostStatus.published)).thenReturn(5L);
            when(scheduledPostRepository.countByUserIdAndStatus(userId, ScheduledPostStatus.queued)).thenReturn(2L);
            when(topicRepository.countByUserId(userId)).thenReturn(3L);
            when(analyticsRepository.findAvgEngagementRateByUserId(userId)).thenReturn(Optional.of(3.5f));
            when(geminiUsageService.getUsageSummary()).thenReturn(
                    new GeminiUsageService.GeminiUsageSummary(50, 1400, 500, null));

            DashboardSummaryResponse result = dashboardService.getSummary();

            assertThat(result.totalPosts()).isEqualTo(10);
            assertThat(result.publishedPosts()).isEqualTo(5);
            assertThat(result.scheduledPosts()).isEqualTo(2);
            assertThat(result.totalTopics()).isEqualTo(3);
            assertThat(result.avgEngagementRate()).isEqualTo(3.5f);
            assertThat(result.geminiUsageToday()).isEqualTo(50);
        }
    }
}
