package com.linkedinagent.dto.response;

import java.time.OffsetDateTime;
import java.util.Map;

public record DashboardSummaryResponse(
        long totalPosts,
        long publishedPosts,
        long scheduledPosts,
        long totalTopics,
        float avgEngagementRate,
        int geminiUsageToday,
        int geminiDailyLimit,
        Map<String, Object> recentActivity
) {
}
