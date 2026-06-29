package com.linkedinagent.service;

import com.linkedinagent.dto.response.DashboardSummaryResponse;
import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.entity.enums.ScheduledPostStatus;
import com.linkedinagent.repository.AnalyticsRepository;
import com.linkedinagent.repository.GeneratedPostRepository;
import com.linkedinagent.repository.ScheduledPostRepository;
import com.linkedinagent.repository.TopicRepository;
import com.linkedinagent.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final GeneratedPostRepository generatedPostRepository;
    private final ScheduledPostRepository scheduledPostRepository;
    private final TopicRepository topicRepository;
    private final AnalyticsRepository analyticsRepository;
    private final GeminiUsageService geminiUsageService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        UUID userId = SecurityUtils.getCurrentUserId();

        long totalPosts = generatedPostRepository.countByUserId(userId);
        long publishedPosts = generatedPostRepository.countByUserIdAndStatus(userId, PostStatus.published);
        long scheduledPosts = scheduledPostRepository.countByUserIdAndStatus(userId, ScheduledPostStatus.queued);
        long totalTopics = topicRepository.countByUserId(userId);

        Float avgEngagement = analyticsRepository.findAvgEngagementRateByUserId(userId).orElse(0.0f);

        GeminiUsageService.GeminiUsageSummary geminiUsage = geminiUsageService.getUsageSummary();

        Map<String, Object> recentActivity = new HashMap<>();
        recentActivity.put("postsThisWeek", totalPosts);
        recentActivity.put("engagementTrend", avgEngagement > 3.0 ? "improving" : "stable");

        return new DashboardSummaryResponse(
                totalPosts,
                publishedPosts,
                scheduledPosts,
                totalTopics,
                avgEngagement,
                geminiUsage.todayCount(),
                geminiUsage.dailyLimit(),
                recentActivity
        );
    }
}
