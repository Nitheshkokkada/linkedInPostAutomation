package com.linkedinagent.agent;

import com.linkedinagent.domain.LinkedInShareStats;
import com.linkedinagent.entity.Analytics;
import com.linkedinagent.entity.PublishedPost;
import com.linkedinagent.entity.User;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.exception.AgentException;
import com.linkedinagent.exception.LinkedInTokenExpiredException;
import com.linkedinagent.repository.AnalyticsRepository;
import com.linkedinagent.repository.PublishedPostRepository;
import com.linkedinagent.repository.UserRepository;
import com.linkedinagent.service.AgentLogService;
import com.linkedinagent.service.LinkedInOAuthService;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsAgent {

    public static final String AGENT_NAME = "AnalyticsAgent";

    private final PublishedPostRepository publishedPostRepository;
    private final AnalyticsRepository analyticsRepository;
    private final UserRepository userRepository;
    private final LinkedInOAuthService linkedInOAuthService;
    private final AgentLogService agentLogService;
    private final RestTemplate restTemplate;

    @Transactional
    public void runScheduledFetches() {
        long startMs = System.currentTimeMillis();
        UUID runId = UUID.randomUUID();
        UUID logId = agentLogService.startLog(null, AGENT_NAME, runId,
                "Hourly analytics fetch for all users").getId();

        try {
            List<PublishedPost> recentPosts = publishedPostRepository.findAll().stream()
                    .filter(p -> p.getLinkedinPostId() != null)
                    .filter(p -> p.getPublishedAt() != null)
                    .filter(p -> p.getPublishedAt().isAfter(OffsetDateTime.now().minusDays(7)))
                    .toList();

            int updated = 0;
            for (PublishedPost post : recentPosts) {
                try {
                    fetchAndSaveAnalytics(post);
                    updated++;
                } catch (Exception e) {
                    log.warn("Failed to fetch analytics for publishedPostId={}: {}",
                            post.getId(), e.getMessage());
                }
            }

            agentLogService.completeLog(logId, AgentStatus.success,
                    "Fetched analytics for " + updated + "/" + recentPosts.size() + " posts",
                    null, System.currentTimeMillis() - startMs);

            log.info("AnalyticsFetchJob completed: {}/{} posts updated", updated, recentPosts.size());
        } catch (Exception e) {
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);
            throw new AgentException("Scheduled analytics fetch failed", e);
        }
    }

    @Transactional
    public void fetchForPublishedPost(UUID publishedPostId, String window) {
        long startMs = System.currentTimeMillis();
        UUID runId = UUID.randomUUID();
        UUID logId = agentLogService.startLog(null, AGENT_NAME, runId,
                "Analytics fetch for publishedPostId=" + publishedPostId + ", window=" + window).getId();

        try {
            PublishedPost post = publishedPostRepository.findById(publishedPostId)
                    .orElseThrow(() -> new com.linkedinagent.exception.ResourceNotFoundException(
                            "Published post not found: " + publishedPostId));

            fetchAndSaveAnalytics(post);

            agentLogService.completeLog(logId, AgentStatus.success,
                    "Analytics fetched for window=" + window,
                    null, System.currentTimeMillis() - startMs);
        } catch (Exception e) {
            agentLogService.completeLog(logId, AgentStatus.failed, null, e.getMessage(),
                    System.currentTimeMillis() - startMs);
            if (e instanceof AgentException || e instanceof com.linkedinagent.exception.ResourceNotFoundException) {
                throw e;
            }
            throw new AgentException("Analytics fetch failed for post " + publishedPostId, e);
        }
    }

    @Retry(name = "linkedin", maxAttempts = 3)
    public void fetchAndSaveAnalytics(PublishedPost post) {
        User user = userRepository.findById(post.getUserId())
                .orElseThrow(() -> new AgentException("User not found for analytics fetch"));

        String accessToken = linkedInOAuthService.getDecryptedAccessToken(user.getId());

        LinkedInShareStats stats = fetchShareStats(accessToken, post.getLinkedinPostId());

        Optional<Analytics> existing = analyticsRepository
                .findTopByPublishedPostIdOrderByFetchedAtDesc(post.getId());

        if (existing.isPresent() && isSameStats(existing.get(), stats)) {
            log.debug("No change in analytics for publishedPostId={}", post.getId());
            return;
        }

        float engagementRate = calculateEngagementRate(stats);

        Analytics analytics = Analytics.builder()
                .publishedPostId(post.getId())
                .userId(post.getUserId())
                .impressions(stats.impressions())
                .likes(stats.likes())
                .comments(stats.comments())
                .shares(stats.shares())
                .engagementRate(engagementRate)
                .fetchedAt(OffsetDateTime.now())
                .build();

        analyticsRepository.save(analytics);
    }

    private LinkedInShareStats fetchShareStats(String accessToken, String linkedinPostId) {
        String url = "https://api.linkedin.com/v2/socialActions/urn:li:share:" + linkedinPostId;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<LinkedInShareStats> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(null, headers),
                    LinkedInShareStats.class);

            if (response.getBody() != null) {
                return response.getBody();
            }
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new LinkedInTokenExpiredException("LinkedIn token expired during analytics fetch");
            }
            log.error("LinkedIn analytics API error: {}", e.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to fetch LinkedIn share stats for post {}", linkedinPostId, e);
        }

        return new LinkedInShareStats(0, 0, 0, 0);
    }

    private float calculateEngagementRate(LinkedInShareStats stats) {
        if (stats.impressions() == 0) {
            return 0.0f;
        }
        int totalEngagement = stats.likes() + stats.comments() + stats.shares();
        return (float) totalEngagement / stats.impressions() * 100;
    }

    private boolean isSameStats(Analytics existing, LinkedInShareStats stats) {
        return existing.getImpressions() == stats.impressions()
                && existing.getLikes() == stats.likes()
                && existing.getComments() == stats.comments()
                && existing.getShares() == stats.shares();
    }
}
