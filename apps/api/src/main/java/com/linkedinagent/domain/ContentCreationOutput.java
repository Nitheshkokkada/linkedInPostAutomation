package com.linkedinagent.domain;

import java.util.UUID;

public record ContentCreationOutput(
        UUID postId,
        UUID researchId,
        UUID topicId,
        PostContent content,
        String fullText,
        int wordCount
) {
}
