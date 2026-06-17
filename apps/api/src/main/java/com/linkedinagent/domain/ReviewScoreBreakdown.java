package com.linkedinagent.domain;

public record ReviewScoreBreakdown(
        int grammarClarity,
        int originality,
        int readability,
        int technicalAccuracy,
        double maxSimilarity,
        boolean tooSimilar
) {
    public int total() {
        return grammarClarity + originality + readability + technicalAccuracy;
    }
}
