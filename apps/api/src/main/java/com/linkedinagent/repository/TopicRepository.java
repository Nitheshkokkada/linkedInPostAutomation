package com.linkedinagent.repository;

import com.linkedinagent.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findByUserIdAndIsActiveTrueOrderByPriorityAsc(UUID userId);

    List<Topic> findByUserId(UUID userId);

    long countByUserId(UUID userId);
}
