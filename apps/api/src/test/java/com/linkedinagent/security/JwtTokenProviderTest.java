package com.linkedinagent.security;

import com.linkedinagent.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.setSecretKey("test-secret-key-minimum-32-characters-long");
        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    void generatesAndValidatesAccessToken() {
        UUID userId = UUID.randomUUID();
        String email = "user@example.com";

        String token = jwtTokenProvider.generateAccessToken(userId, email);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(token)).isFalse();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(email);
    }

    @Test
    void generatesAndValidatesRefreshToken() {
        UUID userId = UUID.randomUUID();
        String email = "user@example.com";

        String token = jwtTokenProvider.generateRefreshToken(userId, email);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(token)).isFalse();
    }

    @Test
    void rejectsInvalidToken() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.value")).isFalse();
    }

    @Test
    void rejectsShortSecretKey() {
        AppProperties properties = new AppProperties();
        properties.setSecretKey("too-short");
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        assertThatThrownBy(() -> provider.generateAccessToken(UUID.randomUUID(), "a@b.com"))
                .isInstanceOf(IllegalStateException.class);
    }
}
