package com.linkedinagent.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedinagent.config.AppProperties;
import com.linkedinagent.domain.GeminiGenerateContentResponse;
import com.linkedinagent.exception.AgentException;
import com.linkedinagent.exception.RateLimitException;
import com.linkedinagent.service.GeminiUsageService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiImageClient {

    private static final String GENERATE_CONTENT_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final GeminiUsageService geminiUsageService;
    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public GeminiImageClient(
            RestTemplate restTemplate,
            AppProperties appProperties,
            GeminiUsageService geminiUsageService,
            RateLimiterRegistry rateLimiterRegistry,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
        this.geminiUsageService = geminiUsageService;
        this.rateLimiter = rateLimiterRegistry.rateLimiter("gemini");
        this.objectMapper = objectMapper;
    }

    public byte[] generateImage(String prompt) {
        geminiUsageService.checkDailyBudget();

        try {
            byte[] imageBytes = RateLimiter.decorateSupplier(rateLimiter, () -> callGeminiImageApi(prompt)).get();
            geminiUsageService.incrementUsage();
            return imageBytes;
        } catch (RequestNotPermitted e) {
            log.warn("Gemini rate limit exceeded during image generation");
            throw new RateLimitException("Gemini rate limit exceeded. Try again shortly.");
        } catch (RateLimitException | AgentException e) {
            throw e;
        } catch (Exception e) {
            if (e.getCause() instanceof RequestNotPermitted) {
                throw new RateLimitException("Gemini rate limit exceeded. Try again shortly.");
            }
            log.error("Gemini image generation failed", e);
            throw new AgentException("Gemini image generation failed", e);
        }
    }

    private byte[] callGeminiImageApi(String prompt) {
        String apiKey = appProperties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AgentException("Gemini API key is not configured");
        }

        String url = UriComponentsBuilder.fromUriString(GENERATE_CONTENT_URL)
                .queryParam("key", apiKey)
                .buildAndExpand(appProperties.getGemini().getModel())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
        )));
        body.put("generationConfig", Map.of(
                "responseModalities", List.of("TEXT", "IMAGE")
        ));

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    String.class);

            if (response.getBody() == null || response.getBody().isBlank()) {
                throw new AgentException("Gemini returned empty image response");
            }

            return extractPngBytes(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Gemini image API failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AgentException("Gemini image API failed: " + e.getStatusCode());
        } catch (AgentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini image API error", e);
            throw new AgentException("Gemini image API call failed", e);
        }
    }

    private byte[] extractPngBytes(String responseBody) throws Exception {
        GeminiGenerateContentResponse response = objectMapper.readValue(
                responseBody, GeminiGenerateContentResponse.class);

        if (response.candidates() == null || response.candidates().isEmpty()) {
            throw new AgentException("Gemini image response contained no candidates");
        }

        GeminiGenerateContentResponse.Content content = response.candidates().getFirst().content();
        if (content == null || content.parts() == null) {
            throw new AgentException("Gemini image response contained no parts");
        }

        for (GeminiGenerateContentResponse.Part part : content.parts()) {
            if (part.inlineData() != null && part.inlineData().data() != null) {
                String mimeType = part.inlineData().mimeType();
                if (mimeType != null && !mimeType.contains("png") && !mimeType.contains("image")) {
                    log.warn("Unexpected image mime type: {}", mimeType);
                }
                return Base64.getDecoder().decode(part.inlineData().data());
            }
        }

        throw new AgentException("Gemini image response did not contain inline image data");
    }
}
