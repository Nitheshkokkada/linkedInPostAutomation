package com.linkedinagent.service;

import com.linkedinagent.dto.response.ScheduledPostResponse;
import com.linkedinagent.entity.ScheduledPost;
import com.linkedinagent.entity.enums.ScheduledPostStatus;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.ScheduledPostRepository;
import com.linkedinagent.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduledPostRepository scheduledPostRepository;

    @Transactional(readOnly = true)
    public List<ScheduledPostResponse> getAllScheduledPosts() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return scheduledPostRepository.findByUserIdOrderByScheduledForAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduledPostResponse> getScheduledPostsForDay(OffsetDateTime day) {
        UUID userId = SecurityUtils.getCurrentUserId();
        OffsetDateTime dayStart = day.withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime dayEnd = dayStart.plusDays(1);
        return scheduledPostRepository.findByUserIdAndScheduledForBetween(userId, dayStart, dayEnd).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduledPostResponse getScheduledPostById(UUID scheduledPostId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ScheduledPost post = scheduledPostRepository.findById(scheduledPostId)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled post not found"));
        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public long getQueuedCount() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return scheduledPostRepository.countByUserIdAndStatus(userId, ScheduledPostStatus.queued);
    }

    @Transactional
    public void cancelScheduledPost(UUID scheduledPostId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ScheduledPost post = scheduledPostRepository.findById(scheduledPostId)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled post not found"));

        if (post.getStatus() != ScheduledPostStatus.queued) {
            throw new com.linkedinagent.exception.ConflictException("Only queued posts can be cancelled");
        }

        scheduledPostRepository.delete(post);
        log.info("Cancelled scheduled post {} for user={}", scheduledPostId, userId);
    }

    private ScheduledPostResponse toResponse(ScheduledPost post) {
        return new ScheduledPostResponse(
                post.getId(),
                post.getPostId(),
                post.getImageId(),
                post.getScheduledFor(),
                post.getStatus(),
                post.getRetryCount(),
                post.getLastError(),
                post.getCreatedAt()
        );
    }
}
