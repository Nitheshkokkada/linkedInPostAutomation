package com.linkedinagent.controller;

import com.linkedinagent.dto.response.AnalyticsResponse;
import com.linkedinagent.dto.response.PublishedPostResponse;
import com.linkedinagent.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<List<AnalyticsResponse>> getAllAnalytics() {
        return ResponseEntity.ok(analyticsService.getAllAnalytics());
    }

    @GetMapping("/post/{publishedPostId}")
    public ResponseEntity<List<AnalyticsResponse>> getAnalyticsForPost(
            @PathVariable UUID publishedPostId) {
        return ResponseEntity.ok(analyticsService.getAnalyticsForPost(publishedPostId));
    }

    @GetMapping("/engagement-rate")
    public ResponseEntity<Map<String, Object>> getAverageEngagementRate() {
        var rate = analyticsService.getAverageEngagementRate();
        return ResponseEntity.ok(Map.of(
                "avgEngagementRate", rate.orElse(0.0f),
                "hasData", rate.isPresent()
        ));
    }

    @GetMapping("/published-posts")
    public ResponseEntity<List<PublishedPostResponse>> getPublishedPosts() {
        return ResponseEntity.ok(analyticsService.getPublishedPosts());
    }

    @GetMapping("/published-count")
    public ResponseEntity<Map<String, Long>> getPublishedCount() {
        return ResponseEntity.ok(Map.of("count", analyticsService.getPublishedCount()));
    }
}
