package com.linkedinagent.domain;

import com.linkedinagent.entity.enums.PostStatus;

import java.util.Map;
import java.util.UUID;

public record ReviewOutput(
        UUID postId,
        int qualityScore,
        boolean approved,
        PostStatus status,
        ReviewScoreBreakdown breakdown,
        Map<String, Object> qualityFeedback,
        String rejectionReason
) {
}
