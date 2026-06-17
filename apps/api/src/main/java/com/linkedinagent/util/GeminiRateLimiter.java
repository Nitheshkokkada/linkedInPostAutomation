package com.linkedinagent.util;

import com.linkedinagent.exception.RateLimitException;
import com.linkedinagent.service.GeminiUsageService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
public class GeminiRateLimiter {

    private final RateLimiter rateLimiter;
    private final GeminiUsageService geminiUsageService;
    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;

    public GeminiRateLimiter(
            RateLimiterRegistry rateLimiterRegistry,
            GeminiUsageService geminiUsageService,
            ChatModel chatModel,
            EmbeddingModel embeddingModel) {
        this.rateLimiter = rateLimiterRegistry.rateLimiter("gemini");
        this.geminiUsageService = geminiUsageService;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }

    public ChatResponse call(Prompt prompt) {
        geminiUsageService.checkDailyBudget();

        try {
            ChatResponse response = RateLimiter.decorateSupplier(rateLimiter, () -> chatModel.call(prompt)).get();
            geminiUsageService.incrementUsage();
            return response;
        } catch (RequestNotPermitted e) {
            log.warn("Gemini rate limit exceeded ({} RPM)", rateLimiter.getRateLimiterConfig().getLimitForPeriod());
            throw new RateLimitException("Gemini rate limit exceeded. Try again shortly.");
        } catch (Exception e) {
            if (e.getCause() instanceof RequestNotPermitted) {
                throw new RateLimitException("Gemini rate limit exceeded. Try again shortly.");
            }
            throw e;
        }
    }

    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        return executeWithRateLimit(() -> embeddingModel.embed(texts));
    }

    public <T> T executeWithRateLimit(Supplier<T> geminiCall) {
        geminiUsageService.checkDailyBudget();

        try {
            T result = RateLimiter.decorateSupplier(rateLimiter, geminiCall).get();
            geminiUsageService.incrementUsage();
            return result;
        } catch (RequestNotPermitted e) {
            log.warn("Gemini rate limit exceeded");
            throw new RateLimitException("Gemini rate limit exceeded. Try again shortly.");
        }
    }
}
