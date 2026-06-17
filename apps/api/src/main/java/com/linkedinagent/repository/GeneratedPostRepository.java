package com.linkedinagent.repository;

import com.linkedinagent.entity.GeneratedPost;
import com.linkedinagent.entity.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeneratedPostRepository extends JpaRepository<GeneratedPost, UUID> {

    Optional<GeneratedPost> findByIdAndUserId(UUID id, UUID userId);

    Page<GeneratedPost> findByUserId(UUID userId, Pageable pageable);

    Page<GeneratedPost> findByUserIdAndStatus(UUID userId, PostStatus status, Pageable pageable);

    List<GeneratedPost> findByUserIdAndStatusIn(UUID userId, List<PostStatus> statuses);

    @Query("SELECT gp FROM GeneratedPost gp WHERE gp.userId = :userId ORDER BY gp.createdAt DESC")
    List<GeneratedPost> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    long countByUserIdAndStatus(UUID userId, PostStatus status);

    long countByUserId(UUID userId);

    boolean existsByResearchId(UUID researchId);
}
