package com.linkedinagent.util;

import com.linkedinagent.config.AppProperties;
import com.linkedinagent.domain.TavilySearchResponse;
import com.linkedinagent.exception.AgentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TavilySearchClient {

    private static final String TAVILY_SEARCH_URL = "https://api.tavily.com/search";

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public TavilySearchClient(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    @Retry(name = "gemini")
    public TavilySearchResponse search(String query) {
        String apiKey = appProperties.getTavily().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AgentException("Tavily API key is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("api_key", apiKey);
        body.put("query", query);
        body.put("max_results", 5);
        body.put("search_depth", "basic");

        try {
            ResponseEntity<TavilySearchResponse> response = restTemplate.postForEntity(
                    TAVILY_SEARCH_URL,
                    new HttpEntity<>(body, headers),
                    TavilySearchResponse.class);

            if (response.getBody() == null || response.getBody().results() == null) {
                throw new AgentException("Tavily returned empty search results");
            }
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Tavily search failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AgentException("Tavily search failed: " + e.getStatusCode());
        } catch (AgentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Tavily search error for query={}", query, e);
            throw new AgentException("Tavily search failed", e);
        }
    }

    public String formatResultsForPrompt(TavilySearchResponse response) {
        if (response.results() == null || response.results().isEmpty()) {
            return "No search results found.";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < response.results().size(); i++) {
            TavilySearchResponse.TavilyResult result = response.results().get(i);
            sb.append(i + 1).append(". Title: ").append(result.title()).append('\n');
            sb.append("   URL: ").append(result.url()).append('\n');
            sb.append("   Snippet: ").append(truncate(result.content(), 500)).append("\n\n");
        }
        return sb.toString();
    }

    public TavilySearchResponse.TavilyResult topResult(TavilySearchResponse response) {
        List<TavilySearchResponse.TavilyResult> results = response.results();
        if (results == null || results.isEmpty()) {
            throw new AgentException("No Tavily results to process");
        }
        return results.getFirst();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
