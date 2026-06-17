package com.linkedinagent.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TavilySearchResponse(
        List<TavilyResult> results
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TavilyResult(
            String title,
            String url,
            String content
    ) {
    }
}
