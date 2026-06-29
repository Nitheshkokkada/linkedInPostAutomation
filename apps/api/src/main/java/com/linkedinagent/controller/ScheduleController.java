package com.linkedinagent.controller;

import com.linkedinagent.dto.response.ScheduledPostResponse;
import com.linkedinagent.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<List<ScheduledPostResponse>> getAllScheduledPosts() {
        return ResponseEntity.ok(scheduleService.getAllScheduledPosts());
    }

    @GetMapping("/day")
    public ResponseEntity<List<ScheduledPostResponse>> getScheduledPostsForDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime day) {
        return ResponseEntity.ok(scheduleService.getScheduledPostsForDay(day));
    }

    @GetMapping("/{scheduledPostId}")
    public ResponseEntity<ScheduledPostResponse> getScheduledPostById(
            @PathVariable UUID scheduledPostId) {
        return ResponseEntity.ok(scheduleService.getScheduledPostById(scheduledPostId));
    }

    @GetMapping("/queued-count")
    public ResponseEntity<Long> getQueuedCount() {
        return ResponseEntity.ok(scheduleService.getQueuedCount());
    }

    @DeleteMapping("/{scheduledPostId}")
    public ResponseEntity<Void> cancelScheduledPost(@PathVariable UUID scheduledPostId) {
        scheduleService.cancelScheduledPost(scheduledPostId);
        return ResponseEntity.noContent().build();
    }
}
