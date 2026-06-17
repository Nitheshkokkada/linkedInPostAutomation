package com.linkedinagent.controller;

import com.linkedinagent.dto.auth.UserResponse;
import com.linkedinagent.security.SecurityUtils;
import com.linkedinagent.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final AuthService authService;

    @GetMapping
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getUserById(SecurityUtils.getCurrentUserId()));
    }
}
