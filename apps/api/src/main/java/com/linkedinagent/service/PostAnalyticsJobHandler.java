package com.linkedinagent.service;

import com.linkedinagent.entity.enums.AgentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Per-post analytics fetch handler.
 * TODO(phase-11): delegate to {@code AnalyticsAgent}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostAnalyticsJobHandler {

    private final AgentLogService agentLogService;

    public void execute(UUID publishedPostId, String window) {
        long startMs = System.currentTimeMillis();
        UUID runId = UUID.randomUUID();
        UUID logId = agentLogService.startLog(null, "PostAnalyticsJob", runId,
                "Analytics fetch for publishedPostId=" + publishedPostId + ", window=" + window).getId();

        try {
            // TODO(phase-11): analyticsAgent.fetchForPublishedPost(publishedPostId, window)
            log.debug("PostAnalyticsJob scheduled fetch — publishedPostId={}, window={}", publishedPostId, window);

            agentLogService.completeLog(logId, AgentStatus.success,
                    "Analytics fetch stub completed for window=" + window,
                    null, System.currentTimeMillis() - startMs);
        } catch (Exception e) {
            log.error("PostAnalyticsJob failed for publishedPostId={}", publishedPostId, e);
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);
            throw e;
        }
    }
}
