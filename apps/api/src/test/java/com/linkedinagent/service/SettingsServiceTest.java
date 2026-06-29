package com.linkedinagent.service;

import com.linkedinagent.dto.auth.UserResponse;
import com.linkedinagent.dto.request.SettingsUpdateRequest;
import com.linkedinagent.entity.User;
import com.linkedinagent.entity.enums.PostingMode;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.UserRepository;
import com.linkedinagent.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SettingsService settingsService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("test@test.com")
                .fullName("Test User")
                .timezone("UTC")
                .postingMode(PostingMode.draft)
                .build();
    }

    @Test
    void getSettings_shouldReturnUserSettings() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserResponse result = settingsService.getSettings();

            assertThat(result.email()).isEqualTo("test@test.com");
            assertThat(result.timezone()).isEqualTo("UTC");
        }
    }

    @Test
    void updateSettings_shouldUpdateFields() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            SettingsUpdateRequest request = new SettingsUpdateRequest(
                    "Updated Name", "America/New_York", "auto", null);
            UserResponse result = settingsService.updateSettings(request);

            assertThat(result.fullName()).isEqualTo("Updated Name");
            assertThat(result.timezone()).isEqualTo("America/New_York");
            assertThat(result.postingMode()).isEqualTo(PostingMode.auto);
        }
    }

    @Test
    void updateSettings_shouldThrowWhenUserNotFound() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            SettingsUpdateRequest request = new SettingsUpdateRequest("Name", null, null, null);
            assertThatThrownBy(() -> settingsService.updateSettings(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
