package com.linkedinagent.service;

import com.linkedinagent.dto.request.PostUpdateRequest;
import com.linkedinagent.dto.response.PostResponse;
import com.linkedinagent.entity.GeneratedPost;
import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.GeneratedPostRepository;
import com.linkedinagent.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final GeneratedPostRepository generatedPostRepository;

    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPosts(int page, int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return generatedPostRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByStatus(PostStatus status, int page, int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return generatedPostRepository.findByUserIdAndStatus(userId, status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(UUID postId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GeneratedPost post = generatedPostRepository.findByIdAndUserId(postId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return toResponse(post);
    }

    @Transactional
    public PostResponse updatePost(UUID postId, PostUpdateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GeneratedPost post = generatedPostRepository.findByIdAndUserId(postId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (request.title() != null) post.setTitle(request.title());
        if (request.fullText() != null) post.setFullText(request.fullText());
        if (request.status() != null) post.setStatus(request.status());

        GeneratedPost saved = generatedPostRepository.save(post);
        return toResponse(saved);
    }

    @Transactional
    public PostResponse approvePost(UUID postId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GeneratedPost post = generatedPostRepository.findByIdAndUserId(postId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        post.setStatus(PostStatus.approved);
        GeneratedPost saved = generatedPostRepository.save(post);
        return toResponse(saved);
    }

    @Transactional
    public PostResponse rejectPost(UUID postId, String reason) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GeneratedPost post = generatedPostRepository.findByIdAndUserId(postId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        post.setStatus(PostStatus.rejected);
        post.setRejectionReason(reason);
        GeneratedPost saved = generatedPostRepository.save(post);
        return toResponse(saved);
    }

    @Transactional
    public void deletePost(UUID postId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GeneratedPost post = generatedPostRepository.findByIdAndUserId(postId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        generatedPostRepository.delete(post);
        log.info("Deleted post {} for user={}", postId, userId);
    }

    private PostResponse toResponse(GeneratedPost post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getHook(),
                post.getBody(),
                post.getKeyTakeaways(),
                post.getCallToAction(),
                post.getFullText(),
                post.getQualityScore(),
                post.getQualityFeedback(),
                post.getStatus(),
                post.getRejectionReason(),
                post.getWordCount(),
                post.getTopicId(),
                post.getResearchId(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
