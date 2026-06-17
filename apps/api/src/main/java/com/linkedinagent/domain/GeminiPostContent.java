package com.linkedinagent.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiPostContent(
        String title,
        String hook,
        String body,
        List<String> keyTakeaways,
        String callToAction,
        List<String> hashtags
) {
    public PostContent toPostContent() {
        return new PostContent(hook, body, keyTakeaways, callToAction, hashtags);
    }
}
