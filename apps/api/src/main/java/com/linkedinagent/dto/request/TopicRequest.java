package com.linkedinagent.dto.request;

import com.linkedinagent.entity.enums.TopicCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TopicRequest(
        @NotBlank String name,
        @NotNull TopicCategory category,
        @Min(1) @Max(10) int priority
) {
}
