package com.linkedinagent.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiReviewAnalysis(
        int grammarClarity,
        int technicalAccuracy,
        String feedback,
        String rejectionReason
) {
}
