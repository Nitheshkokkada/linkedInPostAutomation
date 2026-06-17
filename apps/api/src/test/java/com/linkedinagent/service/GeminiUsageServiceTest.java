package com.linkedinagent.service;

import com.linkedinagent.config.AppProperties;
import com.linkedinagent.entity.GeminiUsage;
import com.linkedinagent.exception.BudgetExceededException;
import com.linkedinagent.repository.GeminiUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiUsageServiceTest {

    @Mock
    private GeminiUsageRepository geminiUsageRepository;

    private GeminiUsageService geminiUsageService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getGemini().setDailyLimit(1400);
        geminiUsageService = new GeminiUsageService(geminiUsageRepository, appProperties);
    }

    @Test
    void checkDailyBudgetThrowsWhenAtLimit() {
        GeminiUsage usage = GeminiUsage.builder()
                .usageDate(LocalDate.now())
                .callCount(1400)
                .build();
        when(geminiUsageRepository.findByUsageDate(LocalDate.now())).thenReturn(Optional.of(usage));

        assertThatThrownBy(() -> geminiUsageService.checkDailyBudget())
                .isInstanceOf(BudgetExceededException.class);
    }

    @Test
    void checkDailyBudgetPassesWhenUnderLimit() {
        when(geminiUsageRepository.findByUsageDate(LocalDate.now())).thenReturn(Optional.empty());

        geminiUsageService.checkDailyBudget();
    }

    @Test
    void incrementUsageCallsRepository() {
        when(geminiUsageRepository.findByUsageDate(LocalDate.now())).thenReturn(Optional.empty());

        geminiUsageService.incrementUsage();

        verify(geminiUsageRepository).incrementCallCount(LocalDate.now());
    }

    @Test
    void getUsageSummaryReturnsTodayCount() {
        GeminiUsage usage = GeminiUsage.builder()
                .usageDate(LocalDate.now())
                .callCount(42)
                .build();
        when(geminiUsageRepository.findByUsageDate(LocalDate.now())).thenReturn(Optional.of(usage));
        when(geminiUsageRepository.findUsageSince(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(usage));

        GeminiUsageService.GeminiUsageSummary summary = geminiUsageService.getUsageSummary();

        assertThat(summary.todayCount()).isEqualTo(42);
        assertThat(summary.dailyLimit()).isEqualTo(1400);
    }
}
