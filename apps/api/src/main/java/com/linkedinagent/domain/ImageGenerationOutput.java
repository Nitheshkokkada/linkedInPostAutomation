package com.linkedinagent.domain;

import java.util.UUID;

public record ImageGenerationOutput(
        UUID imageId,
        UUID postId,
        String storagePath,
        String publicUrl,
        String promptUsed,
        int width,
        int height
) {
}
