package com.linkedinagent.service;

import com.linkedinagent.dto.response.PostResponse;
import com.linkedinagent.entity.GeneratedPost;
import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.GeneratedPostRepository;
import com.linkedinagent.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private GeneratedPostRepository generatedPostRepository;

    @InjectMocks
    private PostService postService;

    private UUID userId;
    private UUID postId;
    private GeneratedPost post;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        postId = UUID.randomUUID();
        post = GeneratedPost.builder()
                .id(postId)
                .userId(userId)
                .title("Test Post")
                .fullText("Test content")
                .status(PostStatus.draft)
                .wordCount(150)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void getPostById_shouldReturnPostWhenOwner() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(generatedPostRepository.findByIdAndUserId(postId, userId)).thenReturn(Optional.of(post));

            PostResponse result = postService.getPostById(postId);

            assertThat(result.title()).isEqualTo("Test Post");
            assertThat(result.status()).isEqualTo(PostStatus.draft);
        }
    }

    @Test
    void getPostById_shouldThrowWhenNotFound() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(generatedPostRepository.findByIdAndUserId(postId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.getPostById(postId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    void approvePost_shouldUpdateStatus() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(generatedPostRepository.findByIdAndUserId(postId, userId)).thenReturn(Optional.of(post));
            when(generatedPostRepository.save(any(GeneratedPost.class))).thenAnswer(inv -> inv.getArgument(0));

            PostResponse result = postService.approvePost(postId);

            assertThat(result.status()).isEqualTo(PostStatus.approved);
        }
    }

    @Test
    void getAllPosts_shouldReturnPagedPosts() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            Pageable pageable = PageRequest.of(0, 20);
            Page<GeneratedPost> page = new PageImpl<>(List.of(post), pageable, 1);
            when(generatedPostRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

            Page<PostResponse> result = postService.getAllPosts(0, 20);

            assertThat(result.getContent()).hasSize(1);
        }
    }
}
