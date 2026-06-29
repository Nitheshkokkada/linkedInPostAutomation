package com.linkedinagent.dto.request;

import com.linkedinagent.entity.enums.PostStatus;
import jakarta.validation.constraints.NotBlank;

public record PostUpdateRequest(
        String title,
        String fullText,
        PostStatus status
) {
}
