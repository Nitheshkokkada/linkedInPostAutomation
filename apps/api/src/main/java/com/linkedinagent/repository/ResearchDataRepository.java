package com.linkedinagent.repository;

import com.linkedinagent.entity.ResearchData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResearchDataRepository extends JpaRepository<ResearchData, UUID> {

    List<ResearchData> findByTopicIdOrderByFetchedAtDesc(UUID topicId);

    List<ResearchData> findTop5ByTopicIdOrderByRelevanceScoreDesc(UUID topicId);

    @Query("""
            SELECT rd FROM ResearchData rd
            JOIN Topic t ON rd.topicId = t.id
            WHERE rd.id = :researchId AND t.userId = :userId
            """)
    Optional<ResearchData> findByIdAndUserId(
            @Param("researchId") UUID researchId,
            @Param("userId") UUID userId);

    @Query("""
            SELECT rd FROM ResearchData rd
            JOIN Topic t ON rd.topicId = t.id
            WHERE t.userId = :userId
            AND NOT EXISTS (
                SELECT 1 FROM GeneratedPost gp WHERE gp.researchId = rd.id
            )
            ORDER BY rd.relevanceScore DESC NULLS LAST, rd.fetchedAt DESC
            """)
    List<ResearchData> findUnprocessedByUserId(@Param("userId") UUID userId);
}
