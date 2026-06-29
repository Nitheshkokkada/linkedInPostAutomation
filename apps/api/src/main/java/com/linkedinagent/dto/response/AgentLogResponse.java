package com.linkedinagent.dto.response;

import com.linkedinagent.entity.enums.AgentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AgentLogResponse(
        UUID id,
        UUID userId,
        String agentName,
        UUID runId,
        AgentStatus status,
        String inputSummary,
        String outputSummary,
        String errorMessage,
        Long durationMs,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
}
