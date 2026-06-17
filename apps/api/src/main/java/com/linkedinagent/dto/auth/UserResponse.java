package com.linkedinagent.dto.auth;

import com.linkedinagent.entity.enums.PostingMode;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String timezone,
        PostingMode postingMode,
        boolean linkedinConnected
) {
}
