package com.linkedinagent.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiGenerateContentResponse(
        List<Candidate> candidates
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(List<Part> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(
            @JsonProperty("inlineData") InlineData inlineData,
            String text
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InlineData(
            String mimeType,
            String data
    ) {
    }
}
