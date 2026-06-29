package com.linkedinagent.service;

import com.linkedinagent.dto.auth.UserResponse;
import com.linkedinagent.dto.request.SettingsUpdateRequest;
import com.linkedinagent.entity.User;
import com.linkedinagent.entity.enums.PostingMode;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.UserRepository;
import com.linkedinagent.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getSettings() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateSettings(SettingsUpdateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.timezone() != null) user.setTimezone(request.timezone());
        if (request.postingMode() != null) {
            try {
                user.setPostingMode(PostingMode.valueOf(request.postingMode()));
            } catch (IllegalArgumentException e) {
                throw new com.linkedinagent.exception.ConflictException(
                        "Invalid posting mode: " + request.postingMode());
            }
        }
        if (request.preferredPostTime() != null) user.setPreferredPostTime(request.preferredPostTime());

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getTimezone(),
                user.getPostingMode(),
                user.getLinkedinAccessToken() != null && !user.getLinkedinAccessToken().isBlank()
        );
    }
}
