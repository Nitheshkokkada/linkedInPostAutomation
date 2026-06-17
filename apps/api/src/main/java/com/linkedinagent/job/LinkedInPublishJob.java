package com.linkedinagent.job;

import com.linkedinagent.service.LinkedInPublishJobHandler;
import com.linkedinagent.service.QuartzJobScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class LinkedInPublishJob implements Job {

    private final LinkedInPublishJobHandler linkedInPublishJobHandler;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            String scheduledPostId = context.getJobDetail().getJobDataMap().getString(QuartzJobScheduler.SCHEDULED_POST_ID_KEY);
            if (scheduledPostId == null || scheduledPostId.isBlank()) {
                throw new JobExecutionException("Missing scheduledPostId in job data");
            }
            linkedInPublishJobHandler.execute(UUID.fromString(scheduledPostId));
        } catch (JobExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("LinkedInPublishJob failed", e);
            throw new JobExecutionException(e);
        }
    }
}
