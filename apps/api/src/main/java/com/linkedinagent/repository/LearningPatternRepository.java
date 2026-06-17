package com.linkedinagent.repository;

import com.linkedinagent.entity.LearningPattern;
import com.linkedinagent.entity.enums.PatternType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearningPatternRepository extends JpaRepository<LearningPattern, UUID> {

    List<LearningPattern> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT lp FROM LearningPattern lp WHERE lp.userId = :userId ORDER BY lp.createdAt DESC")
    List<LearningPattern> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    List<LearningPattern> findByUserIdAndPatternType(UUID userId, PatternType patternType);

    List<LearningPattern> findByUserIdAndTopicCategory(UUID userId, String topicCategory);
}
