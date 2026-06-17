package com.linkedinagent.controller;

import com.linkedinagent.config.AppProperties;
import com.linkedinagent.domain.AuthTokens;
import com.linkedinagent.dto.auth.AuthResponse;
import com.linkedinagent.dto.auth.LoginRequest;
import com.linkedinagent.dto.auth.RegisterRequest;
import com.linkedinagent.exception.UnauthorizedException;
import com.linkedinagent.security.SecurityUtils;
import com.linkedinagent.service.AuthService;
import com.linkedinagent.service.LinkedInOAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final LinkedInOAuthService linkedInOAuthService;
    private final AppProperties appProperties;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        AuthTokens tokens = authService.register(request);
        setRefreshCookie(response, tokens.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(toAuthResponse(tokens));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthTokens tokens = authService.login(request);
        setRefreshCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(toAuthResponse(tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {

        AuthTokens tokens = authService.refresh(refreshToken);
        setRefreshCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(toAuthResponse(tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/linkedin/connect")
    public ResponseEntity<Void> linkedInConnect(HttpServletResponse response) throws IOException {
        var userId = SecurityUtils.getCurrentUserId();
        String authUrl = linkedInOAuthService.buildAuthorizationUrl(userId);
        response.sendRedirect(authUrl);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authUrl)).build();
    }

    @GetMapping("/linkedin/callback")
    public ResponseEntity<Void> linkedInCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response) throws IOException {

        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            throw new UnauthorizedException("Invalid LinkedIn OAuth callback parameters");
        }

        linkedInOAuthService.handleCallback(code, state);
        String redirectUrl = appProperties.getFrontendUrl() + "/settings?linkedin=connected";
        response.sendRedirect(redirectUrl);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }

    @DeleteMapping("/linkedin/disconnect")
    public ResponseEntity<Void> linkedInDisconnect() {
        linkedInOAuthService.disconnect(SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    private AuthResponse toAuthResponse(AuthTokens tokens) {
        return AuthResponse.of(tokens.accessToken(), tokens.accessExpiresInMs(), tokens.user());
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = buildRefreshCookie(refreshToken);
        cookie.setMaxAge((int) (appProperties.getJwt().getRefreshTokenExpiration() / 1000));
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = buildRefreshCookie("");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private Cookie buildRefreshCookie(String value) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, value);
        cookie.setHttpOnly(true);
        cookie.setSecure("production".equalsIgnoreCase(appProperties.getEnv()));
        cookie.setPath(REFRESH_COOKIE_PATH);
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }
}
