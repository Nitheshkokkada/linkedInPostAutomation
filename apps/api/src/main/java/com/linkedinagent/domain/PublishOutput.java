package com.linkedinagent.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PublishOutput(
        UUID publishedPostId,
        UUID scheduledPostId,
        UUID postId,
        String linkedinPostId,
        String linkedinPostUrl,
        OffsetDateTime publishedAt,
        boolean linkedinPublished
) {
}
