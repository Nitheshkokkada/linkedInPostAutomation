package com.linkedinagent.controller;

import com.linkedinagent.dto.request.TopicRequest;
import com.linkedinagent.dto.response.TopicResponse;
import com.linkedinagent.service.TopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    public ResponseEntity<List<TopicResponse>> getAllTopics() {
        return ResponseEntity.ok(topicService.getAllTopics());
    }

    @GetMapping("/active")
    public ResponseEntity<List<TopicResponse>> getActiveTopics() {
        return ResponseEntity.ok(topicService.getActiveTopics());
    }

    @GetMapping("/{topicId}")
    public ResponseEntity<TopicResponse> getTopicById(@PathVariable UUID topicId) {
        return ResponseEntity.ok(topicService.getTopicById(topicId));
    }

    @PostMapping
    public ResponseEntity<TopicResponse> createTopic(@Valid @RequestBody TopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topicService.createTopic(request));
    }

    @PutMapping("/{topicId}")
    public ResponseEntity<TopicResponse> updateTopic(
            @PathVariable UUID topicId,
            @Valid @RequestBody TopicRequest request) {
        return ResponseEntity.ok(topicService.updateTopic(topicId, request));
    }

    @DeleteMapping("/{topicId}")
    public ResponseEntity<Void> deleteTopic(@PathVariable UUID topicId) {
        topicService.deleteTopic(topicId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{topicId}/toggle-active")
    public ResponseEntity<TopicResponse> toggleActive(@PathVariable UUID topicId) {
        return ResponseEntity.ok(topicService.toggleActive(topicId));
    }
}
