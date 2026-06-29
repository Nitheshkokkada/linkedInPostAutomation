package com.linkedinagent.service;

import com.linkedinagent.dto.response.AgentLogResponse;
import com.linkedinagent.entity.AgentLog;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.repository.AgentLogRepository;
import com.linkedinagent.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentLogRepository agentLogRepository;

    @Transactional(readOnly = true)
    public Page<AgentLogResponse> getAgentLogs(int page, int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        return agentLogRepository.findByUserIdOrderByStartedAtDesc(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AgentLogResponse> getAgentLogsByName(String agentName, int page, int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        return agentLogRepository.findByUserIdAndAgentNameOrderByStartedAtDesc(userId, agentName, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AgentLogResponse> getAgentLogsByStatus(AgentStatus status, int page, int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        return agentLogRepository.findByUserIdAndStatusOrderByStartedAtDesc(userId, status, pageable)
                .map(this::toResponse);
    }

    private AgentLogResponse toResponse(AgentLog log) {
        return new AgentLogResponse(
                log.getId(),
                log.getUserId(),
                log.getAgentName(),
                log.getRunId(),
                log.getStatus(),
                log.getInputSummary(),
                log.getOutputSummary(),
                log.getErrorMessage(),
                log.getDurationMs(),
                log.getStartedAt(),
                log.getFinishedAt()
        );
    }
}
