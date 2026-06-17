package com.linkedinagent.controller;

import com.linkedinagent.config.AppProperties;
import com.linkedinagent.security.SecurityUtils;
import com.linkedinagent.service.GeminiUsageService;
import com.linkedinagent.service.GeminiUsageService.GeminiUsageSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
public class UsageController {

    private final GeminiUsageService geminiUsageService;

    @GetMapping("/gemini")
    public ResponseEntity<GeminiUsageResponse> getGeminiUsage() {
        SecurityUtils.getCurrentUserId();
        GeminiUsageSummary summary = geminiUsageService.getUsageSummary();
        return ResponseEntity.ok(new GeminiUsageResponse(
                summary.todayCount(),
                summary.dailyLimit(),
                summary.monthlyCount(),
                1500,
                summary.asOf()
        ));
    }

    public record GeminiUsageResponse(
            int todayCount,
            int dailyLimit,
            int monthlyCount,
            int platformDailyLimit,
            java.time.OffsetDateTime asOf
    ) {
    }
}
