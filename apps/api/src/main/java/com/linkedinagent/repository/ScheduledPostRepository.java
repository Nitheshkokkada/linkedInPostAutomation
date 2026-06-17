package com.linkedinagent.repository;

import com.linkedinagent.entity.ScheduledPost;
import com.linkedinagent.entity.enums.ScheduledPostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledPostRepository extends JpaRepository<ScheduledPost, UUID> {

    List<ScheduledPost> findByUserIdOrderByScheduledForAsc(UUID userId);

    List<ScheduledPost> findByStatusAndScheduledForBefore(ScheduledPostStatus status, OffsetDateTime before);

    @Query("SELECT sp FROM ScheduledPost sp WHERE sp.userId = :userId " +
           "AND sp.scheduledFor >= :dayStart AND sp.scheduledFor < :dayEnd")
    List<ScheduledPost> findByUserIdAndScheduledForBetween(
            @Param("userId") UUID userId,
            @Param("dayStart") OffsetDateTime dayStart,
            @Param("dayEnd") OffsetDateTime dayEnd);

    long countByUserIdAndStatus(UUID userId, ScheduledPostStatus status);

    boolean existsByPostId(UUID postId);
}
