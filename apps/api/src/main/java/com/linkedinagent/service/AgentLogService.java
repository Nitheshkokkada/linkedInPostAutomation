package com.linkedinagent.service;

import com.linkedinagent.entity.AgentLog;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.exception.AgentException;
import com.linkedinagent.repository.AgentLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentLogService {

    private final AgentLogRepository agentLogRepository;

    @Transactional
    public AgentLog startLog(UUID userId, String agentName, UUID runId, String inputSummary) {
        AgentLog agentLog = AgentLog.builder()
                .userId(userId)
                .agentName(agentName)
                .runId(runId)
                .status(AgentStatus.running)
                .inputSummary(inputSummary)
                .startedAt(OffsetDateTime.now())
                .build();

        try {
            return agentLogRepository.save(agentLog);
        } catch (Exception e) {
            log.error("Failed to create agent log for agent={}, runId={}", agentName, runId, e);
            throw new AgentException("Failed to start agent log", e);
        }
    }

    @Transactional
    public void completeLog(UUID logId, AgentStatus status, String outputSummary, String errorMessage, long durationMs) {
        AgentLog agentLog = agentLogRepository.findById(logId)
                .orElseThrow(() -> new AgentException("Agent log not found: " + logId));

        agentLog.setStatus(status);
        agentLog.setOutputSummary(outputSummary);
        agentLog.setErrorMessage(errorMessage);
        agentLog.setDurationMs(durationMs);
        agentLog.setFinishedAt(OffsetDateTime.now());

        try {
            agentLogRepository.save(agentLog);
        } catch (Exception e) {
            log.error("Failed to update agent log id={}", logId, e);
            throw new AgentException("Failed to complete agent log", e);
        }
    }

    @Transactional
    public int purgeLogsOlderThanDays(int days) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(days);
        try {
            return agentLogRepository.deleteByStartedAtBefore(cutoff);
        } catch (Exception e) {
            log.error("Failed to purge agent logs older than {} days", days, e);
            throw new AgentException("Failed to purge agent logs", e);
        }
    }
}
