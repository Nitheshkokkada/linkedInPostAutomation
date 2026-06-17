package com.linkedinagent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import java.time.OffsetDateTime;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuartzJobSchedulerTest {

    @Mock
    private Scheduler scheduler;

    @InjectMocks
    private QuartzJobScheduler quartzJobScheduler;

    @Test
    void scheduleLinkedInPublishRegistersJobAndTrigger() throws SchedulerException {
        UUID scheduledPostId = UUID.randomUUID();
        OffsetDateTime scheduledFor = OffsetDateTime.now().plusHours(2);
        JobKey jobKey = JobKey.jobKey("linkedin-publish-" + scheduledPostId, QuartzJobScheduler.PUBLISH_JOB_GROUP);

        when(scheduler.checkExists(jobKey)).thenReturn(false);

        String result = quartzJobScheduler.scheduleLinkedInPublish(scheduledPostId, scheduledFor);

        assertThat(result).isEqualTo(jobKey.toString());
        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void cancelLinkedInPublishDeletesExistingJob() throws SchedulerException {
        UUID scheduledPostId = UUID.randomUUID();
        JobKey jobKey = JobKey.jobKey("linkedin-publish-" + scheduledPostId, QuartzJobScheduler.PUBLISH_JOB_GROUP);

        when(scheduler.checkExists(jobKey)).thenReturn(true);

        quartzJobScheduler.cancelLinkedInPublish(scheduledPostId);

        verify(scheduler).deleteJob(eq(jobKey));
    }
}
