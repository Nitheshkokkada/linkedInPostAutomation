package com.linkedinagent.domain;

import java.util.List;

public record PostContent(
        String hook,
        String body,
        List<String> keyTakeaways,
        String callToAction,
        List<String> hashtags
) {
}
