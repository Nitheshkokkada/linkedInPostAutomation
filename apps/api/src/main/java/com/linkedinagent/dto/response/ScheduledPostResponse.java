package com.linkedinagent.dto.response;

import com.linkedinagent.entity.enums.ScheduledPostStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduledPostResponse(
        UUID id,
        UUID postId,
        UUID imageId,
        OffsetDateTime scheduledFor,
        ScheduledPostStatus status,
        int retryCount,
        String lastError,
        OffsetDateTime createdAt
) {
}
