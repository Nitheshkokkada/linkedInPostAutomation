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

@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class PurgeOldLogsJob implements Job {

    private static final int RETENTION_DAYS = 90;

    private final AgentLogService agentLogService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long startMs = System.currentTimeMillis();
        UUID runId = UUID.randomUUID();
        UUID logId = agentLogService.startLog(null, "PurgeOldLogsJob", runId,
                "Purging agent logs older than " + RETENTION_DAYS + " days").getId();

        try {
            int deleted = agentLogService.purgeLogsOlderThanDays(RETENTION_DAYS);

            agentLogService.completeLog(logId, AgentStatus.success,
                    "Deleted " + deleted + " log entries", null, System.currentTimeMillis() - startMs);
            log.info("PurgeOldLogsJob deleted {} entries older than {} days", deleted, RETENTION_DAYS);
        } catch (Exception e) {
            log.error("PurgeOldLogsJob failed", e);
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);
            throw new JobExecutionException(e);
        }
    }
}
