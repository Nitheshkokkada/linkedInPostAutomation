package com.linkedinagent.service;

import com.linkedinagent.dto.response.AgentLogResponse;
import com.linkedinagent.entity.AgentLog;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.repository.AgentLogRepository;
import com.linkedinagent.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentLogRepository agentLogRepository;

    @InjectMocks
    private AgentService agentService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void getAgentLogs_shouldReturnPagedLogs() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            AgentLog log = AgentLog.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .agentName("TopicResearchAgent")
                    .runId(UUID.randomUUID())
                    .status(AgentStatus.success)
                    .inputSummary("Researching topics")
                    .outputSummary("Researched 3 topics")
                    .durationMs(1500L)
                    .startedAt(OffsetDateTime.now())
                    .finishedAt(OffsetDateTime.now())
                    .build();

            Pageable pageable = PageRequest.of(0, 50);
            Page<AgentLog> page = new PageImpl<>(List.of(log), pageable, 1);
            when(agentLogRepository.findByUserIdOrderByStartedAtDesc(eq(userId), any(Pageable.class)))
                    .thenReturn(page);

            Page<AgentLogResponse> result = agentService.getAgentLogs(0, 50);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).agentName()).isEqualTo("TopicResearchAgent");
            assertThat(result.getContent().get(0).status()).isEqualTo(AgentStatus.success);
        }
    }

    @Test
    void getAgentLogsByName_shouldFilterByAgentName() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            AgentLog log = AgentLog.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .agentName("ContentCreationAgent")
                    .runId(UUID.randomUUID())
                    .status(AgentStatus.running)
                    .startedAt(OffsetDateTime.now())
                    .build();

            Pageable pageable = PageRequest.of(0, 50);
            Page<AgentLog> page = new PageImpl<>(List.of(log), pageable, 1);
            when(agentLogRepository.findByUserIdAndAgentNameOrderByStartedAtDesc(
                    eq(userId), eq("ContentCreationAgent"), any(Pageable.class)))
                    .thenReturn(page);

            Page<AgentLogResponse> result = agentService.getAgentLogsByName("ContentCreationAgent", 0, 50);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).agentName()).isEqualTo("ContentCreationAgent");
        }
    }

    @Test
    void getAgentLogsByStatus_shouldFilterByStatus() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            Pageable pageable = PageRequest.of(0, 50);
            Page<AgentLog> page = new PageImpl<>(List.of(), pageable, 0);
            when(agentLogRepository.findByUserIdAndStatusOrderByStartedAtDesc(
                    eq(userId), eq(AgentStatus.failed), any(Pageable.class)))
                    .thenReturn(page);

            Page<AgentLogResponse> result = agentService.getAgentLogsByStatus(AgentStatus.failed, 0, 50);

            assertThat(result.getContent()).isEmpty();
        }
    }
}
