package com.linkedinagent.service;

import com.linkedinagent.config.AppProperties;
import com.linkedinagent.entity.GeminiUsage;
import com.linkedinagent.exception.BudgetExceededException;
import com.linkedinagent.repository.GeminiUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiUsageService {

    private final GeminiUsageRepository geminiUsageRepository;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public void checkDailyBudget() {
        LocalDate today = LocalDate.now();
        int callCount = geminiUsageRepository.findByUsageDate(today)
                .map(GeminiUsage::getCallCount)
                .orElse(0);

        if (callCount >= appProperties.getGemini().getDailyLimit()) {
            log.warn("Gemini daily budget exceeded: {}/{}", callCount, appProperties.getGemini().getDailyLimit());
            throw new BudgetExceededException("Gemini daily call limit reached (" + callCount + " calls)");
        }
    }

    @Transactional
    public void incrementUsage() {
        checkDailyBudget();
        try {
            geminiUsageRepository.incrementCallCount(LocalDate.now());
        } catch (Exception e) {
            log.error("Failed to increment Gemini usage counter", e);
            throw new BudgetExceededException("Unable to track Gemini usage");
        }
    }

    @Transactional(readOnly = true)
    public GeminiUsageSummary getUsageSummary() {
        LocalDate today = LocalDate.now();
        int todayCount = geminiUsageRepository.findByUsageDate(today)
                .map(GeminiUsage::getCallCount)
                .orElse(0);

        LocalDate monthStart = YearMonth.from(today).atDay(1);
        List<GeminiUsage> monthlyRecords = geminiUsageRepository.findUsageSince(monthStart);
        int monthlyCount = monthlyRecords.stream()
                .mapToInt(GeminiUsage::getCallCount)
                .sum();

        return new GeminiUsageSummary(
                todayCount,
                appProperties.getGemini().getDailyLimit(),
                monthlyCount,
                OffsetDateTime.now()
        );
    }

    public record GeminiUsageSummary(
            int todayCount,
            int dailyLimit,
            int monthlyCount,
            OffsetDateTime asOf
    ) {
    }
}
