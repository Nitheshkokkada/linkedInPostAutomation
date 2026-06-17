package com.linkedinagent.agent;

import com.linkedinagent.domain.ScheduleOutput;
import com.linkedinagent.entity.AgentLog;
import com.linkedinagent.entity.GeneratedImage;
import com.linkedinagent.entity.GeneratedPost;
import com.linkedinagent.entity.ScheduledPost;
import com.linkedinagent.entity.User;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.entity.enums.ScheduledPostStatus;
import com.linkedinagent.repository.GeneratedImageRepository;
import com.linkedinagent.repository.GeneratedPostRepository;
import com.linkedinagent.repository.ScheduledPostRepository;
import com.linkedinagent.repository.UserRepository;
import com.linkedinagent.service.AgentLogService;
import com.linkedinagent.service.QuartzJobScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerAgentTest {

    @Mock
    private GeneratedPostRepository generatedPostRepository;

    @Mock
    private GeneratedImageRepository generatedImageRepository;

    @Mock
    private ScheduledPostRepository scheduledPostRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AgentLogService agentLogService;

    @Mock
    private QuartzJobScheduler quartzJobScheduler;

    @InjectMocks
    private SchedulerAgent schedulerAgent;

    @Test
    void runSchedulesPostAndCreatesQuartzJob() {
        UUID userId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        UUID scheduledId = UUID.randomUUID();
        UUID logId = UUID.randomUUID();

        GeneratedPost post = GeneratedPost.builder()
                .id(postId)
                .userId(userId)
                .status(PostStatus.approved)
                .build();

        GeneratedImage image = GeneratedImage.builder().id(imageId).postId(postId).build();
        User user = User.builder()
                .id(userId)
                .timezone("UTC")
                .preferredPostTime(LocalTime.of(9, 0))
                .build();

        when(agentLogService.startLog(any(), anyString(), any(), anyString()))
                .thenReturn(AgentLog.builder().id(logId).build());
        when(generatedPostRepository.findByIdAndUserId(postId, userId)).thenReturn(Optional.of(post));
        when(scheduledPostRepository.existsByPostId(postId)).thenReturn(false);
        when(generatedImageRepository.findByPostId(postId)).thenReturn(Optional.of(image));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scheduledPostRepository.findByUserIdAndScheduledForBetween(
                eq(userId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(java.util.List.of());

        ScheduledPost saved = ScheduledPost.builder()
                .id(scheduledId)
                .postId(postId)
                .imageId(imageId)
                .userId(userId)
                .scheduledFor(OffsetDateTime.now().plusDays(1))
                .status(ScheduledPostStatus.queued)
                .build();
        when(scheduledPostRepository.save(any(ScheduledPost.class))).thenReturn(saved);
        when(quartzJobScheduler.scheduleLinkedInPublish(any(), any())).thenReturn("publish.linkedin-publish-" + scheduledId);
        when(generatedPostRepository.save(any(GeneratedPost.class))).thenAnswer(inv -> inv.getArgument(0));

        ScheduleOutput output = schedulerAgent.run(userId, runId, postId);

        assertThat(output.scheduledPostId()).isEqualTo(scheduledId);
        assertThat(output.postId()).isEqualTo(postId);
        assertThat(output.imageId()).isEqualTo(imageId);
        assertThat(output.quartzJobKey()).contains("linkedin-publish");

        ArgumentCaptor<GeneratedPost> postCaptor = ArgumentCaptor.forClass(GeneratedPost.class);
        verify(generatedPostRepository).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getStatus()).isEqualTo(PostStatus.scheduled);

        verify(quartzJobScheduler).scheduleLinkedInPublish(eq(scheduledId), any(OffsetDateTime.class));
        verify(agentLogService).completeLog(eq(logId), eq(AgentStatus.success), anyString(), eq(null), anyLong());
    }
}
