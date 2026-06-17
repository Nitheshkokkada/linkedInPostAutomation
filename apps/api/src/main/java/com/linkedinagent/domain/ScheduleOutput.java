package com.linkedinagent.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleOutput(
        UUID scheduledPostId,
        UUID postId,
        UUID imageId,
        OffsetDateTime scheduledFor,
        String quartzJobKey
) {
}
