package com.linkedinagent.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AnalyticsResponse(
        UUID id,
        UUID publishedPostId,
        int impressions,
        int likes,
        int comments,
        int shares,
        float engagementRate,
        OffsetDateTime fetchedAt
) {
}
