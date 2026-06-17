package com.linkedinagent.repository;

import com.linkedinagent.entity.Analytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalyticsRepository extends JpaRepository<Analytics, UUID> {

    List<Analytics> findByPublishedPostIdOrderByFetchedAtDesc(UUID publishedPostId);

    Optional<Analytics> findTopByPublishedPostIdOrderByFetchedAtDesc(UUID publishedPostId);

    List<Analytics> findByUserIdOrderByFetchedAtDesc(UUID userId);

    @Query("SELECT a FROM Analytics a WHERE a.userId = :userId ORDER BY a.fetchedAt DESC")
    List<Analytics> findRecentByUserId(@Param("userId") UUID userId);

    @Query("SELECT AVG(a.engagementRate) FROM Analytics a WHERE a.userId = :userId")
    Optional<Float> findAvgEngagementRateByUserId(@Param("userId") UUID userId);
}
