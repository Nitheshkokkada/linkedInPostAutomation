package com.linkedinagent.service;

import com.linkedinagent.agent.LinkedInPublishingAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LinkedInPublishJobHandler {

    private final LinkedInPublishingAgent linkedInPublishingAgent;

    public void execute(UUID scheduledPostId) {
        linkedInPublishingAgent.publish(scheduledPostId);
    }
}
