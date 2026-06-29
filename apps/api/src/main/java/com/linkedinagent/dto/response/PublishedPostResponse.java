package com.linkedinagent.dto.response;

import java.util.List;
import java.util.UUID;

public record PublishedPostResponse(
        UUID id,
        UUID scheduledPostId,
        String linkedinPostId,
        String linkedinPostUrl,
        java.time.OffsetDateTime publishedAt,
        UUID postId,
        String title,
        String fullText,
        Integer qualityScore
) {
}
