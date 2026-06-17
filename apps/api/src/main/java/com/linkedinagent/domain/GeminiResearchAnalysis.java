package com.linkedinagent.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResearchAnalysis(
        String summary,
        List<String> keyConcepts,
        float relevanceScore
) {
}
