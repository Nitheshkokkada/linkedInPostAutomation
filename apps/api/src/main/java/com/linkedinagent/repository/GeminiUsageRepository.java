package com.linkedinagent.repository;

import com.linkedinagent.entity.GeminiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeminiUsageRepository extends JpaRepository<GeminiUsage, UUID> {

    Optional<GeminiUsage> findByUsageDate(LocalDate usageDate);

    @Modifying
    @Query(value = "INSERT INTO gemini_usage (id, usage_date, call_count, last_updated) " +
                   "VALUES (gen_random_uuid(), :date, 1, now()) " +
                   "ON CONFLICT (usage_date) " +
                   "DO UPDATE SET call_count = gemini_usage.call_count + 1, last_updated = now()",
           nativeQuery = true)
    void incrementCallCount(@Param("date") LocalDate date);

    @Query("SELECT gu FROM GeminiUsage gu WHERE gu.usageDate >= :startDate ORDER BY gu.usageDate ASC")
    List<GeminiUsage> findUsageSince(@Param("startDate") LocalDate startDate);
}
