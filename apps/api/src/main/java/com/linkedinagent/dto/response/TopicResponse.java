package com.linkedinagent.dto.response;

import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.entity.enums.TopicCategory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TopicResponse(
        UUID id,
        String name,
        TopicCategory category,
        boolean isActive,
        int priority,
        OffsetDateTime createdAt
) {
}
