package com.linkedinagent.controller;

import com.linkedinagent.dto.auth.UserResponse;
import com.linkedinagent.dto.request.SettingsUpdateRequest;
import com.linkedinagent.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public ResponseEntity<UserResponse> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PatchMapping
    public ResponseEntity<UserResponse> updateSettings(@Valid @RequestBody SettingsUpdateRequest request) {
        return ResponseEntity.ok(settingsService.updateSettings(request));
    }
}
