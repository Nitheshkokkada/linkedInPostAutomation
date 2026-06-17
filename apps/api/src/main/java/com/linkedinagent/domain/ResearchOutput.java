package com.linkedinagent.domain;

import java.util.List;
import java.util.UUID;

public record ResearchOutput(
        UUID topicId,
        String sourceUrl,
        List<String> keyConcepts,
        String summary,
        float relevanceScore
) {
}
