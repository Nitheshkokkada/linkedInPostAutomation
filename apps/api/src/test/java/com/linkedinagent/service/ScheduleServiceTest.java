package com.linkedinagent.service;

import com.linkedinagent.entity.ScheduledPost;
import com.linkedinagent.entity.enums.ScheduledPostStatus;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.ScheduledPostRepository;
import com.linkedinagent.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduledPostRepository scheduledPostRepository;

    @InjectMocks
    private com.linkedinagent.service.ScheduleService scheduleService;

    private UUID userId;
    private UUID scheduledPostId;
    private ScheduledPost scheduledPost;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        scheduledPostId = UUID.randomUUID();
        scheduledPost = ScheduledPost.builder()
                .id(scheduledPostId)
                .userId(userId)
                .postId(UUID.randomUUID())
                .scheduledFor(OffsetDateTime.now().plusHours(2))
                .status(ScheduledPostStatus.queued)
                .retryCount(0)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void getAllScheduledPosts_shouldReturnUserPosts() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(scheduledPostRepository.findByUserIdOrderByScheduledForAsc(userId))
                    .thenReturn(List.of(scheduledPost));

            var result = scheduleService.getAllScheduledPosts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo(ScheduledPostStatus.queued);
        }
    }

    @Test
    void cancelScheduledPost_shouldDeleteWhenQueued() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(scheduledPostRepository.findById(scheduledPostId))
                    .thenReturn(Optional.of(scheduledPost));

            scheduleService.cancelScheduledPost(scheduledPostId);

            verify(scheduledPostRepository, times(1)).delete(scheduledPost);
        }
    }

    @Test
    void cancelScheduledPost_shouldThrowWhenNotQueued() {
        scheduledPost.setStatus(ScheduledPostStatus.published);
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(scheduledPostRepository.findById(scheduledPostId))
                    .thenReturn(Optional.of(scheduledPost));

            assertThatThrownBy(() -> scheduleService.cancelScheduledPost(scheduledPostId))
                    .isInstanceOf(com.linkedinagent.exception.ConflictException.class);
        }
    }
}
