package com.linkedinagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedinagent.config.AppProperties;
import com.linkedinagent.domain.AuthTokens;
import com.linkedinagent.dto.auth.LoginRequest;
import com.linkedinagent.dto.auth.RegisterRequest;
import com.linkedinagent.dto.auth.UserResponse;
import com.linkedinagent.entity.enums.PostingMode;
import com.linkedinagent.service.AuthService;
import com.linkedinagent.service.LinkedInOAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private LinkedInOAuthService linkedInOAuthService;

    @MockBean
    private AppProperties appProperties;

    @Test
    void registerReturnsCreatedWithTokens() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse user = new UserResponse(userId, "user@example.com", "Test User", "UTC", PostingMode.draft, false);
        AuthTokens tokens = new AuthTokens("access-token", "refresh-token", 900_000L, user);

        when(authService.register(any(RegisterRequest.class))).thenReturn(tokens);
        when(appProperties.getEnv()).thenReturn("development");
        when(appProperties.getJwt()).thenReturn(new AppProperties.Jwt());

        RegisterRequest request = new RegisterRequest("user@example.com", "password123", "Test User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("user@example.com"))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void loginReturnsOkWithTokens() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse user = new UserResponse(userId, "user@example.com", "Test User", "UTC", PostingMode.draft, false);
        AuthTokens tokens = new AuthTokens("access-token", "refresh-token", 900_000L, user);

        when(authService.login(any(LoginRequest.class))).thenReturn(tokens);
        when(appProperties.getEnv()).thenReturn("development");
        when(appProperties.getJwt()).thenReturn(new AppProperties.Jwt());

        LoginRequest request = new LoginRequest("user@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void logoutClearsRefreshCookie() throws Exception {
        when(appProperties.getEnv()).thenReturn("development");

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    @Test
    void registerRejectsInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("not-an-email", "password123", "Test User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
