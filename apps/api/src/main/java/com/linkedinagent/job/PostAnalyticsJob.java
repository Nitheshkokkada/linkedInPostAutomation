package com.linkedinagent.job;

import com.linkedinagent.service.PostAnalyticsJobHandler;
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
public class PostAnalyticsJob implements Job {

    private final PostAnalyticsJobHandler postAnalyticsJobHandler;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            String publishedPostId = context.getJobDetail().getJobDataMap()
                    .getString(QuartzJobScheduler.PUBLISHED_POST_ID_KEY);
            String window = context.getJobDetail().getJobDataMap()
                    .getString(QuartzJobScheduler.ANALYTICS_WINDOW_KEY);

            if (publishedPostId == null || publishedPostId.isBlank()) {
                throw new JobExecutionException("Missing publishedPostId in job data");
            }

            postAnalyticsJobHandler.execute(UUID.fromString(publishedPostId), window);
        } catch (JobExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("PostAnalyticsJob failed", e);
            throw new JobExecutionException(e);
        }
    }
}
