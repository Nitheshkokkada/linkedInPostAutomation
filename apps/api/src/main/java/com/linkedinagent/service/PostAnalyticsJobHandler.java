package com.linkedinagent.service;



import com.linkedinagent.agent.AnalyticsAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostAnalyticsJobHandler {

    private final AnalyticsAgent analyticsAgent;

    public void execute(UUID publishedPostId, String window) {
        try {
            analyticsAgent.fetchForPublishedPost(publishedPostId, window);
        } catch (Exception e) {
            log.error("PostAnalyticsJob failed for publishedPostId={}", publishedPostId, e);
            throw e;
        }
    }
}
