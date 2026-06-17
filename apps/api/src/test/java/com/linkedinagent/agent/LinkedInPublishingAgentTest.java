package com.linkedinagent.agent;

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
import com.linkedinagent.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkedInPublishingAgentTest {

    @Mock
    private ScheduledPostRepository scheduledPostRepository;
    @Mock
    private GeneratedPostRepository generatedPostRepository;
    @Mock
    private GeneratedImageRepository generatedImageRepository;
    @Mock
    private PublishedPostRepository publishedPostRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LinkedInOAuthService linkedInOAuthService;
    @Mock
    private LinkedInApiClient linkedInApiClient;
    @Mock
    private SupabaseStorageClient supabaseStorageClient;
    @Mock
    private AgentLogService agentLogService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private QuartzJobScheduler quartzJobScheduler;
    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private LinkedInPublishingAgent linkedInPublishingAgent;

    private UUID userId;
    private UUID postId;
    private UUID scheduledPostId;
    private UUID publishedId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        postId = UUID.randomUUID();
        scheduledPostId = UUID.randomUUID();
        publishedId = UUID.randomUUID();
    }

    @Test
    void publishAutoModePostsToLinkedIn() {
        ScheduledPost scheduledPost = scheduledPost(scheduledPostId, postId, userId);
        GeneratedPost post = post(postId, userId);
        User user = user(userId, PostingMode.auto);
        GeneratedImage image = image(postId);

        when(scheduledPostRepository.findById(scheduledPostId)).thenReturn(Optional.of(scheduledPost));
        when(agentLogService.startLog(any(), anyString(), any(), anyString()))
                .thenReturn(com.linkedinagent.entity.AgentLog.builder().id(UUID.randomUUID()).build());
        when(generatedPostRepository.findByIdAndUserId(postId, userId)).thenReturn(Optional.of(post));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(linkedInOAuthService.getDecryptedAccessToken(userId)).thenReturn("access-token");
        when(linkedInApiClient.toPersonUrn("profile-123")).thenReturn("urn:li:person:profile-123");
        when(generatedImageRepository.findByPostId(postId)).thenReturn(Optional.of(image));
        when(supabaseStorageClient.download("images/path.png")).thenReturn(new byte[]{1, 2, 3});
        when(linkedInApiClient.uploadImageAndGetAssetUrn(anyString(), anyString(), any(byte[].class)))
                .thenReturn("urn:li:digitalmediaAsset:123");
        when(linkedInApiClient.publishImagePost(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("urn:li:share:999");
        when(linkedInApiClient.buildPostUrl("urn:li:share:999"))
                .thenReturn("https://www.linkedin.com/feed/update/urn:li:share:999");

        PublishedPost saved = PublishedPost.builder()
                .id(publishedId)
                .scheduledPostId(scheduledPostId)
                .userId(userId)
                .linkedinPostId("urn:li:share:999")
                .linkedinPostUrl("https://www.linkedin.com/feed/update/urn:li:share:999")
                .publishedAt(OffsetDateTime.now())
                .build();
        when(publishedPostRepository.save(any(PublishedPost.class))).thenReturn(saved);
        when(scheduledPostRepository.save(any(ScheduledPost.class))).thenAnswer(inv -> inv.getArgument(0));
        when(generatedPostRepository.save(any(GeneratedPost.class))).thenAnswer(inv -> inv.getArgument(0));

        PublishOutput output = linkedInPublishingAgent.publish(scheduledPostId);

        assertThat(output.linkedinPublished()).isTrue();
        assertThat(output.linkedinPostId()).isEqualTo("urn:li:share:999");
        verify(quartzJobScheduler).schedulePostAnalytics(eq(publishedId), any(OffsetDateTime.class), eq("24h"));
        verify(quartzJobScheduler).schedulePostAnalytics(eq(publishedId), any(OffsetDateTime.class), eq("72h"));
        verify(quartzJobScheduler).schedulePostAnalytics(eq(publishedId), any(OffsetDateTime.class), eq("7d"));
        verify(linkedInApiClient).publishImagePost(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void publishDraftModeSkipsLinkedIn() {
        ScheduledPost scheduledPost = scheduledPost(scheduledPostId, postId, userId);
        GeneratedPost post = post(postId, userId);
        User user = user(userId, PostingMode.draft);

        when(scheduledPostRepository.findById(scheduledPostId)).thenReturn(Optional.of(scheduledPost));
        when(agentLogService.startLog(any(), anyString(), any(), anyString()))
                .thenReturn(com.linkedinagent.entity.AgentLog.builder().id(UUID.randomUUID()).build());
        when(generatedPostRepository.findByIdAndUserId(postId, userId)).thenReturn(Optional.of(post));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        PublishedPost saved = PublishedPost.builder()
                .id(publishedId)
                .scheduledPostId(scheduledPostId)
                .userId(userId)
                .publishedAt(OffsetDateTime.now())
                .build();
        when(publishedPostRepository.save(any(PublishedPost.class))).thenReturn(saved);
        when(scheduledPostRepository.save(any(ScheduledPost.class))).thenAnswer(inv -> inv.getArgument(0));
        when(generatedPostRepository.save(any(GeneratedPost.class))).thenAnswer(inv -> inv.getArgument(0));

        PublishOutput output = linkedInPublishingAgent.publish(scheduledPostId);

        assertThat(output.linkedinPublished()).isFalse();
        assertThat(output.linkedinPostId()).isNull();
        verify(linkedInApiClient, never()).publishImagePost(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(quartzJobScheduler, never()).schedulePostAnalytics(any(), any(), anyString());
    }

    private ScheduledPost scheduledPost(UUID id, UUID postId, UUID userId) {
        return ScheduledPost.builder()
                .id(id)
                .postId(postId)
                .userId(userId)
                .status(ScheduledPostStatus.queued)
                .scheduledFor(OffsetDateTime.now())
                .retryCount(0)
                .build();
    }

    private GeneratedPost post(UUID id, UUID userId) {
        return GeneratedPost.builder()
                .id(id)
                .userId(userId)
                .title("Test Post")
                .fullText("Post body")
                .status(PostStatus.scheduled)
                .build();
    }

    private User user(UUID id, PostingMode mode) {
        return User.builder()
                .id(id)
                .email("user@example.com")
                .linkedinProfileId("profile-123")
                .postingMode(mode)
                .build();
    }

    private GeneratedImage image(UUID postId) {
        return GeneratedImage.builder()
                .postId(postId)
                .storagePath("images/path.png")
                .build();
    }
}
