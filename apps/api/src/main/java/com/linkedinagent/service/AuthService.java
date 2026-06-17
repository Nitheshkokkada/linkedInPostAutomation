package com.linkedinagent.service;

import com.linkedinagent.domain.AuthTokens;
import com.linkedinagent.dto.auth.LoginRequest;
import com.linkedinagent.dto.auth.RegisterRequest;
import com.linkedinagent.dto.auth.UserResponse;
import com.linkedinagent.entity.User;
import com.linkedinagent.entity.enums.PostingMode;
import com.linkedinagent.exception.ConflictException;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.exception.UnauthorizedException;
import com.linkedinagent.repository.UserRepository;
import com.linkedinagent.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthTokens register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }

        User user = User.builder()
                .email(email)
                .hashedPassword(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .postingMode(PostingMode.draft)
                .build();

        try {
            user = userRepository.save(user);
        } catch (Exception e) {
            log.error("Failed to save user during registration for email={}", email, e);
            throw new ConflictException("Unable to register user");
        }

        log.info("User registered: id={}", user.getId());
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthTokens login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        log.info("User logged in: id={}", user.getId());
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthTokens refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.debug("Token refreshed for user id={}", userId);
        return issueTokens(user);
    }

    private AuthTokens issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());
        long expiresIn = jwtTokenProvider.getAccessTokenExpirationMs();
        return new AuthTokens(accessToken, refreshToken, expiresIn, toUserResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserResponse(user);
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getTimezone(),
                user.getPostingMode(),
                user.getLinkedinAccessToken() != null && !user.getLinkedinAccessToken().isBlank()
        );
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }
}
