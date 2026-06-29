package com.linkedinagent.service;

import com.linkedinagent.dto.request.TopicRequest;
import com.linkedinagent.dto.response.TopicResponse;
import com.linkedinagent.entity.Topic;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.TopicRepository;
import com.linkedinagent.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    @Transactional(readOnly = true)
    public List<TopicResponse> getAllTopics() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return topicRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> getActiveTopics() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return topicRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TopicResponse getTopicById(UUID topicId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Topic topic = topicRepository.findById(topicId)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
        return toResponse(topic);
    }

    @Transactional
    public TopicResponse createTopic(TopicRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Topic topic = Topic.builder()
                .userId(userId)
                .name(request.name())
                .category(request.category())
                .priority(request.priority())
                .isActive(true)
                .build();
        Topic saved = topicRepository.save(topic);
        log.info("Created topic {} for user={}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Transactional
    public TopicResponse updateTopic(UUID topicId, TopicRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Topic topic = topicRepository.findById(topicId)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        topic.setName(request.name());
        topic.setCategory(request.category());
        topic.setPriority(request.priority());

        Topic saved = topicRepository.save(topic);
        return toResponse(saved);
    }

    @Transactional
    public void deleteTopic(UUID topicId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Topic topic = topicRepository.findById(topicId)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));
        topicRepository.delete(topic);
        log.info("Deleted topic {} for user={}", topicId, userId);
    }

    @Transactional
    public TopicResponse toggleActive(UUID topicId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Topic topic = topicRepository.findById(topicId)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        topic.setIsActive(!topic.getIsActive());
        Topic saved = topicRepository.save(topic);
        return toResponse(saved);
    }

    private TopicResponse toResponse(Topic topic) {
        return new TopicResponse(
                topic.getId(),
                topic.getName(),
                topic.getCategory(),
                topic.getIsActive(),
                topic.getPriority(),
                topic.getCreatedAt()
        );
    }
}
