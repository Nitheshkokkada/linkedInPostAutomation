package com.linkedinagent.controller;

import com.linkedinagent.dto.request.PostUpdateRequest;
import com.linkedinagent.dto.response.PostResponse;
import com.linkedinagent.entity.enums.PostStatus;
import com.linkedinagent.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getAllPosts(page, size));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<PostResponse>> getPostsByStatus(
            @PathVariable PostStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getPostsByStatus(status, page, size));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable UUID postId) {
        return ResponseEntity.ok(postService.getPostById(postId));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable UUID postId,
            @RequestBody PostUpdateRequest request) {
        return ResponseEntity.ok(postService.updatePost(postId, request));
    }

    @PostMapping("/{postId}/approve")
    public ResponseEntity<PostResponse> approvePost(@PathVariable UUID postId) {
        return ResponseEntity.ok(postService.approvePost(postId));
    }

    @PostMapping("/{postId}/reject")
    public ResponseEntity<PostResponse> rejectPost(
            @PathVariable UUID postId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(postService.rejectPost(postId, reason));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID postId) {
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
}
