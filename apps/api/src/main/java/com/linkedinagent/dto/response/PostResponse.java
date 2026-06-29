package com.linkedinagent.dto.response;

import com.linkedinagent.entity.enums.PostStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PostResponse(
        UUID id,
        String title,
        String hook,
        String body,
        List<String> keyTakeaways,
        String callToAction,
        String fullText,
        Integer qualityScore,
        Map<String, Object> qualityFeedback,
        PostStatus status,
        String rejectionReason,
        Integer wordCount,
        UUID topicId,
        UUID researchId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
