package com.linkedinagent.config;

import com.linkedinagent.job.AnalyticsFetchJob;
import com.linkedinagent.job.ContentGenerationJob;
import com.linkedinagent.job.PurgeOldLogsJob;
import com.linkedinagent.job.TopicResearchJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class QuartzConfig {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @Bean
    public JobDetail topicResearchJobDetail() {
        return JobBuilder.newJob(TopicResearchJob.class)
                .withIdentity("topicResearchJob")
                .withDescription("Daily topic research at 06:00 UTC")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger topicResearchJobTrigger(JobDetail topicResearchJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(topicResearchJobDetail)
                .withIdentity("topicResearchJobTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 6 * * ?").inTimeZone(UTC))
                .build();
    }

    @Bean
    public JobDetail contentGenerationJobDetail() {
        return JobBuilder.newJob(ContentGenerationJob.class)
                .withIdentity("contentGenerationJob")
                .withDescription("Daily content generation at 07:00 UTC")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger contentGenerationJobTrigger(JobDetail contentGenerationJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(contentGenerationJobDetail)
                .withIdentity("contentGenerationJobTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 7 * * ?").inTimeZone(UTC))
                .build();
    }

    @Bean
    public JobDetail analyticsFetchJobDetail() {
        return JobBuilder.newJob(AnalyticsFetchJob.class)
                .withIdentity("analyticsFetchJob")
                .withDescription("Hourly analytics fetch")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger analyticsFetchJobTrigger(JobDetail analyticsFetchJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(analyticsFetchJobDetail)
                .withIdentity("analyticsFetchJobTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?").inTimeZone(UTC))
                .build();
    }

    @Bean
    public JobDetail purgeOldLogsJobDetail() {
        return JobBuilder.newJob(PurgeOldLogsJob.class)
                .withIdentity("purgeOldLogsJob")
                .withDescription("Weekly purge of agent logs older than 90 days")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger purgeOldLogsJobTrigger(JobDetail purgeOldLogsJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(purgeOldLogsJobDetail)
                .withIdentity("purgeOldLogsJobTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 3 ? * SUN").inTimeZone(UTC))
                .build();
    }
}
