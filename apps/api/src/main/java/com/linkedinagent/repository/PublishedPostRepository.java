package com.linkedinagent.repository;

import com.linkedinagent.entity.PublishedPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublishedPostRepository extends JpaRepository<PublishedPost, UUID> {

    List<PublishedPost> findByUserIdOrderByPublishedAtDesc(UUID userId);

    Optional<PublishedPost> findByLinkedinPostId(String linkedinPostId);

    Optional<PublishedPost> findByScheduledPostId(UUID scheduledPostId);

    long countByUserId(UUID userId);
}
