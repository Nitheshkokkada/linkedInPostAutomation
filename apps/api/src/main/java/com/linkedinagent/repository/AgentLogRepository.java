package com.linkedinagent.repository;

import com.linkedinagent.entity.AgentLog;
import com.linkedinagent.entity.enums.AgentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AgentLogRepository extends JpaRepository<AgentLog, UUID> {

    Page<AgentLog> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    Page<AgentLog> findByUserIdAndAgentNameOrderByStartedAtDesc(UUID userId, String agentName, Pageable pageable);

    Page<AgentLog> findByUserIdAndStatusOrderByStartedAtDesc(UUID userId, AgentStatus status, Pageable pageable);

    List<AgentLog> findByRunId(UUID runId);

    @Query("SELECT al FROM AgentLog al WHERE al.userId = :userId ORDER BY al.startedAt DESC")
    List<AgentLog> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM AgentLog al WHERE al.startedAt < :cutoff")
    int deleteByStartedAtBefore(@Param("cutoff") OffsetDateTime cutoff);
}
