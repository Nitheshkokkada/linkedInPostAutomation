package com.linkedinagent.service;

import com.linkedinagent.config.AppProperties;
import com.linkedinagent.domain.AuthTokens;
import com.linkedinagent.dto.auth.LoginRequest;
import com.linkedinagent.dto.auth.RegisterRequest;
import com.linkedinagent.entity.User;
import com.linkedinagent.exception.ConflictException;
import com.linkedinagent.exception.UnauthorizedException;
import com.linkedinagent.repository.UserRepository;
import com.linkedinagent.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setSecretKey("test-secret-key-minimum-32-characters-long");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900_000L);
    }

    @Test
    void registerCreatesUserAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", "Test User");
        UUID userId = UUID.randomUUID();
        User savedUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .fullName("Test User")
                .hashedPassword("hashed")
                .build();

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateAccessToken(userId, "user@example.com")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(userId, "user@example.com")).thenReturn("refresh-token");

        AuthTokens tokens = authService.register(request);

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-token");
        assertThat(tokens.user().email()).isEqualTo("user@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerThrowsWhenEmailExists() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", "Test User");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokensOnSuccess() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("user@example.com").fullName("Test User").build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(userId, "user@example.com")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(userId, "user@example.com")).thenReturn("refresh-token");

        AuthTokens tokens = authService.login(request);

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void loginThrowsOnBadCredentials() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void refreshThrowsWhenTokenMissing() {
        assertThatThrownBy(() -> authService.refresh(null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshThrowsWhenTokenInvalid() {
        when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
