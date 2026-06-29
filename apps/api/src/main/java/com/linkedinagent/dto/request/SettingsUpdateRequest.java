package com.linkedinagent.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record SettingsUpdateRequest(
        String fullName,
        String timezone,
        String postingMode,
        LocalTime preferredPostTime
) {
}
