package com.linkedinagent.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiPatternAnalysis(
        List<PatternData> patterns
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PatternData(
            String type,
            @JsonProperty("topic_category") String topicCategory,
            @JsonProperty("content_features") Map<String, Object> contentFeatures,
            @JsonProperty("avg_engagement_rate") Float avgEngagementRate,
            @JsonProperty("sample_size") Integer sampleSize,
            String insight
    ) {
    }
}
