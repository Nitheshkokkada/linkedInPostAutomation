package com.linkedinagent.repository;

import com.linkedinagent.entity.GeneratedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeneratedImageRepository extends JpaRepository<GeneratedImage, UUID> {

    Optional<GeneratedImage> findByPostId(UUID postId);

    boolean existsByPostId(UUID postId);
}
