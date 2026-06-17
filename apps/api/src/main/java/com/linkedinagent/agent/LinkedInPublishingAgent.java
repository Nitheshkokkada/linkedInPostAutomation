package com.linkedinagent.agent;

import com.linkedinagent.config.AppProperties;
import com.linkedinagent.domain.PublishOutput;
import com.linkedinagent.entity.GeneratedImage;
import com.linkedinagent.entity.GeneratedPost;
import com.linkedinagent.entity.PublishedPost;
import com.linkedinagent.entity.ScheduledPost;
import com.linkedinagent.entity.User;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.entity.enums.PostingMode;
import com.linkedinagent.entity.enums.ScheduledPostStatus;
import com.linkedinagent.exception.AgentException;
import com.linkedinagent.exception.LinkedInApiException;
import com.linkedinagent.exception.LinkedInTokenExpiredException;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.GeneratedImageRepository;
import com.linkedinagent.repository.GeneratedPostRepository;
import com.linkedinagent.repository.PublishedPostRepository;
import com.linkedinagent.repository.ScheduledPostRepository;
import com.linkedinagent.repository.UserRepository;
import com.linkedinagent.service.AgentLogService;
import com.linkedinagent.service.LinkedInOAuthService;
import com.linkedinagent.service.NotificationService;
import com.linkedinagent.service.QuartzJobScheduler;
import com.linkedinagent.util.LinkedInApiClient;
import com.linkedinagent.util.SupabaseStorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkedInPublishingAgent {

    public static final String AGENT_NAME = "LinkedInPublishingAgent";
    private static final String MANUAL_APPROVED_KEY = "manual_publish_approved";
    private static final String MANUAL_SENT_AT_KEY = "manual_approval_sent_at";

    private final ScheduledPostRepository scheduledPostRepository;
    private final GeneratedPostRepository generatedPostRepository;
    private final GeneratedImageRepository generatedImageRepository;
    private final PublishedPostRepository publishedPostRepository;
    private final UserRepository userRepository;
    private final LinkedInOAuthService linkedInOAuthService;
    private final LinkedInApiClient linkedInApiClient;
    private final SupabaseStorageClient supabaseStorageClient;
    private final AgentLogService agentLogService;
    private final NotificationService notificationService;
    private final QuartzJobScheduler quartzJobScheduler;
    private final AppProperties appProperties;

    @Transactional
    public PublishOutput publish(UUID scheduledPostId) {
        long startMs = System.currentTimeMillis();
        ScheduledPost scheduledPost = scheduledPostRepository.findById(scheduledPostId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled post not found"));

        UUID userId = scheduledPost.getUserId();
        UUID runId = UUID.randomUUID();
        UUID logId = agentLogService.startLog(userId, AGENT_NAME, runId,
                "Publishing scheduled post " + scheduledPostId).getId();

        try {
            if (scheduledPost.getStatus() != ScheduledPostStatus.queued
                    && scheduledPost.getStatus() != ScheduledPostStatus.processing) {
                throw new AgentException("Scheduled post cannot be published: " + scheduledPost.getStatus());
            }

            scheduledPost.setStatus(ScheduledPostStatus.processing);
            scheduledPostRepository.save(scheduledPost);

            GeneratedPost post = generatedPostRepository.findByIdAndUserId(scheduledPost.getPostId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Generated post not found"));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            PublishOutput output = switch (user.getPostingMode()) {
                case draft -> publishLocally(scheduledPost, post, user);
                case manual -> publishManualMode(scheduledPost, post, user);
                case auto -> publishToLinkedIn(scheduledPost, post, user);
            };

            agentLogService.completeLog(logId, AgentStatus.success,
                    buildSuccessSummary(output),
                    null, System.currentTimeMillis() - startMs);

            return output;
        } catch (Exception e) {
            handlePublishFailure(scheduledPost, userId, e);
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);

            if (e instanceof AgentException || e instanceof ResourceNotFoundException
                    || e instanceof LinkedInApiException) {
                throw e;
            }
            throw new AgentException("LinkedIn publishing failed", e);
        }
    }

    @Transactional
    public void approveManualPublish(UUID userId, UUID postId) {
        GeneratedPost post = generatedPostRepository.findByIdAndUserId(postId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        Map<String, Object> feedback = post.getQualityFeedback() != null
                ? new HashMap<>(post.getQualityFeedback())
                : new HashMap<>();
        feedback.put(MANUAL_APPROVED_KEY, true);
        feedback.put("manual_publish_approved_at", OffsetDateTime.now().toString());
        post.setQualityFeedback(feedback);

        try {
            generatedPostRepository.save(post);
        } catch (Exception e) {
            log.error("Failed to save manual approval for postId={}", postId, e);
            throw new AgentException("Failed to save manual approval", e);
        }
    }

    private PublishOutput publishManualMode(ScheduledPost scheduledPost, GeneratedPost post, User user) {
        if (!isManualApproved(post)) {
            if (scheduledPost.getRetryCount() == 0) {
                return sendManualApprovalRequest(scheduledPost, post, user);
            }
            throw new AgentException("Manual approval not received within 2 hours");
        }
        return publishToLinkedIn(scheduledPost, post, user);
    }

    private PublishOutput sendManualApprovalRequest(
            ScheduledPost scheduledPost, GeneratedPost post, User user) {

        String approvalUrl = appProperties.getFrontendUrl() + "/posts/" + post.getId() + "/approve-publish";
        notificationService.sendManualPublishApproval(user.getEmail(), user.getFullName(), approvalUrl);

        Map<String, Object> feedback = post.getQualityFeedback() != null
                ? new HashMap<>(post.getQualityFeedback())
                : new HashMap<>();
        feedback.put(MANUAL_SENT_AT_KEY, OffsetDateTime.now().toString());
        post.setQualityFeedback(feedback);
        generatedPostRepository.save(post);

        scheduledPost.setStatus(ScheduledPostStatus.queued);
        scheduledPost.setRetryCount(1);
        scheduledPostRepository.save(scheduledPost);

        OffsetDateTime deadline = OffsetDateTime.now().plusHours(2);
        quartzJobScheduler.scheduleLinkedInPublish(scheduledPost.getId(), deadline);

        log.info("Manual approval email sent for postId={}, deadline={}", post.getId(), deadline);
        return new PublishOutput(null, scheduledPost.getId(), post.getId(),
                null, null, null, false);
    }

    private PublishOutput publishLocally(ScheduledPost scheduledPost, GeneratedPost post, User user) {
        PublishedPost published = savePublishedPost(scheduledPost, user.getId(), null, null);
        finalizeSuccessfulPublish(scheduledPost, post, published);
        log.info("Draft mode local publish completed for postId={}", post.getId());
        return toOutput(published, post.getId(), false);
    }

    private PublishOutput publishToLinkedIn(ScheduledPost scheduledPost, GeneratedPost post, User user) {
        String accessToken = linkedInOAuthService.getDecryptedAccessToken(user.getId());
        String personUrn = linkedInApiClient.toPersonUrn(user.getLinkedinProfileId());

        GeneratedImage image = generatedImageRepository.findByPostId(post.getId())
                .orElseThrow(() -> new AgentException("Generated image not found for post"));

        if (image.getStoragePath() == null || image.getStoragePath().isBlank()) {
            throw new AgentException("Image storage path is missing");
        }

        byte[] imageBytes = supabaseStorageClient.download(image.getStoragePath());
        String assetUrn = linkedInApiClient.uploadImageAndGetAssetUrn(accessToken, personUrn, imageBytes);

        String postText = post.getFullText() != null ? post.getFullText() : "";
        String linkedinPostId = linkedInApiClient.publishImagePost(
                accessToken, personUrn, assetUrn, postText, post.getTitle());
        String linkedinPostUrl = linkedInApiClient.buildPostUrl(linkedinPostId);

        PublishedPost published = savePublishedPost(scheduledPost, user.getId(), linkedinPostId, linkedinPostUrl);
        finalizeSuccessfulPublish(scheduledPost, post, published);
        scheduleAnalyticsJobs(published.getId());

        log.info("Published to LinkedIn: postId={}, linkedinPostId={}", post.getId(), linkedinPostId);
        return toOutput(published, post.getId(), true);
    }

    private PublishedPost savePublishedPost(
            ScheduledPost scheduledPost,
            UUID userId,
            String linkedinPostId,
            String linkedinPostUrl) {

        PublishedPost published = PublishedPost.builder()
                .scheduledPostId(scheduledPost.getId())
                .userId(userId)
                .linkedinPostId(linkedinPostId)
                .linkedinPostUrl(linkedinPostUrl)
                .publishedAt(OffsetDateTime.now())
                .build();

        try {
            return publishedPostRepository.save(published);
        } catch (Exception e) {
            log.error("Failed to save published post for scheduledPostId={}", scheduledPost.getId(), e);
            throw new AgentException("Failed to save published post", e);
        }
    }

    private void finalizeSuccessfulPublish(
            ScheduledPost scheduledPost,
            GeneratedPost post,
            PublishedPost published) {

        scheduledPost.setStatus(ScheduledPostStatus.published);
        scheduledPost.setLastError(null);
        scheduledPostRepository.save(scheduledPost);

        post.setStatus(PostStatus.published);
        generatedPostRepository.save(post);
    }

    private void scheduleAnalyticsJobs(UUID publishedPostId) {
        OffsetDateTime now = OffsetDateTime.now();
        quartzJobScheduler.schedulePostAnalytics(publishedPostId, now.plusHours(24), "24h");
        quartzJobScheduler.schedulePostAnalytics(publishedPostId, now.plusHours(72), "72h");
        quartzJobScheduler.schedulePostAnalytics(publishedPostId, now.plusDays(7), "7d");
    }

    private void handlePublishFailure(ScheduledPost scheduledPost, UUID userId, Exception e) {
        scheduledPost.setStatus(ScheduledPostStatus.failed);
        scheduledPost.setRetryCount(scheduledPost.getRetryCount() + 1);
        scheduledPost.setLastError(e.getMessage());

        try {
            scheduledPostRepository.save(scheduledPost);
        } catch (Exception saveError) {
            log.error("Failed to persist failed publish status", saveError);
        }

        if (e instanceof LinkedInTokenExpiredException) {
            clearLinkedInToken(userId);
            userRepository.findById(userId).ifPresent(user ->
                    notificationService.sendLinkedInTokenExpired(user.getEmail(), user.getFullName()));
        }
    }

    private void clearLinkedInToken(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLinkedinAccessToken(null);
            try {
                userRepository.save(user);
            } catch (Exception e) {
                log.error("Failed to clear LinkedIn token for userId={}", userId, e);
            }
        });
    }

    private boolean isManualApproved(GeneratedPost post) {
        if (post.getQualityFeedback() == null) {
            return false;
        }
        Object approved = post.getQualityFeedback().get(MANUAL_APPROVED_KEY);
        return Boolean.TRUE.equals(approved);
    }

    private PublishOutput toOutput(PublishedPost published, UUID postId, boolean linkedinPublished) {
        return new PublishOutput(
                published.getId(),
                published.getScheduledPostId(),
                postId,
                published.getLinkedinPostId(),
                published.getLinkedinPostUrl(),
                published.getPublishedAt(),
                linkedinPublished
        );
    }

    private String buildSuccessSummary(PublishOutput output) {
        if (output.linkedinPublished()) {
            return "Published to LinkedIn: " + output.linkedinPostId();
        }
        if (output.publishedPostId() != null) {
            return "Local publish completed (draft mode)";
        }
        return "Manual approval email sent — awaiting user confirmation";
    }
}
