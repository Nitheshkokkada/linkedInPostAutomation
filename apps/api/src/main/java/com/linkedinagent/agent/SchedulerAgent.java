package com.linkedinagent.agent;

import com.linkedinagent.domain.ScheduleOutput;
import com.linkedinagent.entity.GeneratedImage;
import com.linkedinagent.entity.GeneratedPost;
import com.linkedinagent.entity.ScheduledPost;
import com.linkedinagent.entity.User;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.entity.enums.ScheduledPostStatus;
import com.linkedinagent.exception.AgentException;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.GeneratedImageRepository;
import com.linkedinagent.repository.GeneratedPostRepository;
import com.linkedinagent.repository.ScheduledPostRepository;
import com.linkedinagent.repository.UserRepository;
import com.linkedinagent.service.AgentLogService;
import com.linkedinagent.service.QuartzJobScheduler;
import com.linkedinagent.util.ScheduleTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerAgent {

    public static final String AGENT_NAME = "SchedulerAgent";

    private final GeneratedPostRepository generatedPostRepository;
    private final GeneratedImageRepository generatedImageRepository;
    private final ScheduledPostRepository scheduledPostRepository;
    private final UserRepository userRepository;
    private final AgentLogService agentLogService;
    private final QuartzJobScheduler quartzJobScheduler;

    @Transactional
    public ScheduleOutput run(UUID userId, UUID runId, UUID postId) {
        long startMs = System.currentTimeMillis();
        UUID logId = agentLogService.startLog(userId, AGENT_NAME, runId,
                "Scheduling post " + postId).getId();

        try {
            GeneratedPost post = generatedPostRepository.findByIdAndUserId(postId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

            if (post.getStatus() != PostStatus.approved) {
                throw new AgentException("Only approved posts can be scheduled (status: " + post.getStatus() + ")");
            }

            if (scheduledPostRepository.existsByPostId(postId)) {
                throw new AgentException("Post is already scheduled");
            }

            GeneratedImage image = generatedImageRepository.findByPostId(postId)
                    .orElseThrow(() -> new AgentException("Post must have a generated image before scheduling"));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            OffsetDateTime scheduledFor = ScheduleTimeUtil.resolveNextAvailableSlot(user, scheduledPostRepository);

            ScheduledPost scheduledPost = ScheduledPost.builder()
                    .postId(postId)
                    .imageId(image.getId())
                    .userId(userId)
                    .scheduledFor(scheduledFor)
                    .status(ScheduledPostStatus.queued)
                    .build();

            try {
                scheduledPost = scheduledPostRepository.save(scheduledPost);
            } catch (Exception e) {
                log.error("Failed to save scheduled post for postId={}", postId, e);
                throw new AgentException("Failed to save scheduled post", e);
            }

            String quartzJobKey = quartzJobScheduler.scheduleLinkedInPublish(
                    scheduledPost.getId(), scheduledFor);

            post.setStatus(PostStatus.scheduled);
            try {
                generatedPostRepository.save(post);
            } catch (Exception e) {
                log.error("Failed to update post status to scheduled for postId={}", postId, e);
                quartzJobScheduler.cancelLinkedInPublish(scheduledPost.getId());
                throw new AgentException("Failed to update post status", e);
            }

            agentLogService.completeLog(logId, AgentStatus.success,
                    "Scheduled for " + scheduledFor, null, System.currentTimeMillis() - startMs);

            log.info("SchedulerAgent completed for post={}, scheduledFor={}", postId, scheduledFor);

            return new ScheduleOutput(
                    scheduledPost.getId(),
                    postId,
                    image.getId(),
                    scheduledFor,
                    quartzJobKey
            );
        } catch (Exception e) {
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);
            if (e instanceof AgentException || e instanceof ResourceNotFoundException) {
                throw e;
            }
            throw new AgentException("Scheduling failed", e);
        }
    }
}
