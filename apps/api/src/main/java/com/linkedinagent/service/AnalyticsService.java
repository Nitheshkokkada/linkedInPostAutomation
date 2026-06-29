package com.linkedinagent.service;

import com.linkedinagent.dto.response.AnalyticsResponse;
import com.linkedinagent.dto.response.PublishedPostResponse;
import com.linkedinagent.entity.Analytics;
import com.linkedinagent.entity.PublishedPost;
import com.linkedinagent.exception.ResourceNotFoundException;
import com.linkedinagent.repository.AnalyticsRepository;
import com.linkedinagent.repository.PublishedPostRepository;
import com.linkedinagent.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final PublishedPostRepository publishedPostRepository;

    @Transactional(readOnly = true)
    public List<AnalyticsResponse> getAnalyticsForPost(UUID publishedPostId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        verifyOwnership(publishedPostId, userId);
        return analyticsRepository.findByPublishedPostIdOrderByFetchedAtDesc(publishedPostId).stream()
                .map(this::toAnalyticsResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnalyticsResponse> getAllAnalytics() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return analyticsRepository.findByUserIdOrderByFetchedAtDesc(userId).stream()
                .map(this::toAnalyticsResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Float> getAverageEngagementRate() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return analyticsRepository.findAvgEngagementRateByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<PublishedPostResponse> getPublishedPosts() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return publishedPostRepository.findByUserIdOrderByPublishedAtDesc(userId).stream()
                .map(this::toPublishedPostResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getPublishedCount() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return publishedPostRepository.countByUserId(userId);
    }

    private void verifyOwnership(UUID publishedPostId, UUID userId) {
        PublishedPost post = publishedPostRepository.findById(publishedPostId)
                .orElseThrow(() -> new ResourceNotFoundException("Published post not found"));
        if (!post.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Published post not found");
        }
    }

    private AnalyticsResponse toAnalyticsResponse(Analytics analytics) {
        return new AnalyticsResponse(
                analytics.getId(),
                analytics.getPublishedPostId(),
                analytics.getImpressions(),
                analytics.getLikes(),
                analytics.getComments(),
                analytics.getShares(),
                analytics.getEngagementRate(),
                analytics.getFetchedAt()
        );
    }

    private PublishedPostResponse toPublishedPostResponse(PublishedPost published) {
        return new PublishedPostResponse(
                published.getId(),
                published.getScheduledPostId(),
                published.getLinkedinPostId(),
                published.getLinkedinPostUrl(),
                published.getPublishedAt(),
                null,
                null,
                null,
                null
        );
    }
}
