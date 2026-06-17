package com.linkedinagent.job;

import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.service.AgentLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Hourly analytics fetch job.
 * TODO(phase-11): delegate to {@code AnalyticsAgent}.
 */
@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class AnalyticsFetchJob implements Job {

    private final AgentLogService agentLogService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long startMs = System.currentTimeMillis();
        UUID runId = UUID.randomUUID();
        UUID logId = agentLogService.startLog(null, "AnalyticsFetchJob", runId,
                "Hourly analytics fetch").getId();

        try {
            // TODO(phase-11): analyticsAgent.runScheduledFetches()
            log.debug("AnalyticsFetchJob tick — full implementation in phase 11");

            agentLogService.completeLog(logId, AgentStatus.success,
                    "Analytics fetch stub completed", null, System.currentTimeMillis() - startMs);
        } catch (Exception e) {
            log.error("AnalyticsFetchJob failed", e);
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);
            throw new JobExecutionException(e);
        }
    }
}
