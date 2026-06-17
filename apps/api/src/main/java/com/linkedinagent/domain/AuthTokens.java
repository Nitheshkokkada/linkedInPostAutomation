package com.linkedinagent.domain;

import com.linkedinagent.dto.auth.UserResponse;

public record AuthTokens(String accessToken, String refreshToken, long accessExpiresInMs, UserResponse user) {
}
