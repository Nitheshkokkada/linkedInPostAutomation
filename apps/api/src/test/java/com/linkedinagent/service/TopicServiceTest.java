package com.linkedinagent.service;

import com.linkedinagent.dto.request.TopicRequest;
import com.linkedinagent.dto.response.TopicResponse;
import com.linkedinagent.entity.Topic;
import com.linkedinagent.entity.enums.TopicCategory;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.TopicRepository;
import com.linkedinagent.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @InjectMocks
    private TopicService topicService;

    private UUID userId;
    private UUID topicId;
    private Topic topic;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        topicId = UUID.randomUUID();
        topic = Topic.builder()
                .id(topicId)
                .userId(userId)
                .name("AI Trends")
                .category(TopicCategory.ai)
                .priority(5)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void getAllTopics_shouldReturnUserTopics() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(topicRepository.findByUserId(userId)).thenReturn(List.of(topic));

            List<TopicResponse> result = topicService.getAllTopics();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("AI Trends");
        }
    }

    @Test
    void createTopic_shouldSaveAndReturn() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> {
                Topic t = invocation.getArgument(0);
                t.setId(UUID.randomUUID());
                return t;
            });

            TopicRequest request = new TopicRequest("AI Trends", TopicCategory.ai, 5);
            TopicResponse result = topicService.createTopic(request);

            assertThat(result.name()).isEqualTo("AI Trends");
            assertThat(result.category()).isEqualTo(TopicCategory.ai);
            verify(topicRepository, times(1)).save(any(Topic.class));
        }
    }

    @Test
    void deleteTopic_shouldDeleteWhenOwner() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));

            topicService.deleteTopic(topicId);

            verify(topicRepository, times(1)).delete(topic);
        }
    }

    @Test
    void deleteTopic_shouldThrowWhenNotFound() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(topicRepository.findById(topicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> topicService.deleteTopic(topicId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
