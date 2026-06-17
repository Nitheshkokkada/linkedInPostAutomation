package com.linkedinagent.service;

import com.linkedinagent.exception.AgentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuartzJobScheduler {

    public static final String PUBLISH_JOB_GROUP = "publish";
    public static final String ANALYTICS_JOB_GROUP = "analytics";
    public static final String SCHEDULED_POST_ID_KEY = "scheduledPostId";
    public static final String PUBLISHED_POST_ID_KEY = "publishedPostId";
    public static final String ANALYTICS_WINDOW_KEY = "analyticsWindow";

    private final Scheduler scheduler;

    public String scheduleLinkedInPublish(UUID scheduledPostId, OffsetDateTime scheduledFor) {
        String jobName = "linkedin-publish-" + scheduledPostId;
        JobKey jobKey = JobKey.jobKey(jobName, PUBLISH_JOB_GROUP);

        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }

            JobDetail jobDetail = JobBuilder.newJob(com.linkedinagent.job.LinkedInPublishJob.class)
                    .withIdentity(jobKey)
                    .usingJobData(SCHEDULED_POST_ID_KEY, scheduledPostId.toString())
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + scheduledPostId, PUBLISH_JOB_GROUP)
                    .startAt(Date.from(scheduledFor.toInstant()))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled LinkedIn publish job {} at {}", jobKey, scheduledFor);
            return jobKey.toString();
        } catch (SchedulerException e) {
            log.error("Failed to schedule LinkedIn publish job for scheduledPostId={}", scheduledPostId, e);
            throw new AgentException("Failed to schedule publish job", e);
        }
    }

    public void cancelLinkedInPublish(UUID scheduledPostId) {
        JobKey jobKey = JobKey.jobKey("linkedin-publish-" + scheduledPostId, PUBLISH_JOB_GROUP);
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("Cancelled LinkedIn publish job {}", jobKey);
            }
        } catch (SchedulerException e) {
            log.error("Failed to cancel publish job for scheduledPostId={}", scheduledPostId, e);
            throw new AgentException("Failed to cancel publish job", e);
        }
    }

    public String schedulePostAnalytics(UUID publishedPostId, OffsetDateTime fetchAt, String window) {
        String jobName = "post-analytics-" + publishedPostId + "-" + window;
        JobKey jobKey = JobKey.jobKey(jobName, ANALYTICS_JOB_GROUP);

        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }

            JobDetail jobDetail = JobBuilder.newJob(com.linkedinagent.job.PostAnalyticsJob.class)
                    .withIdentity(jobKey)
                    .usingJobData(PUBLISHED_POST_ID_KEY, publishedPostId.toString())
                    .usingJobData(ANALYTICS_WINDOW_KEY, window)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + jobName, ANALYTICS_JOB_GROUP)
                    .startAt(Date.from(fetchAt.toInstant()))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled analytics job {} at {} for window {}", jobKey, fetchAt, window);
            return jobKey.toString();
        } catch (SchedulerException e) {
            log.error("Failed to schedule analytics job for publishedPostId={}", publishedPostId, e);
            throw new AgentException("Failed to schedule analytics job", e);
        }
    }
}
