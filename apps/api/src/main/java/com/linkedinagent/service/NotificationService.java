package com.linkedinagent.service;

import com.linkedinagent.config.AppProperties;
import com.linkedinagent.exception.AgentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public void sendLinkedInTokenExpired(String email, String fullName) {
        if (!isMailConfigured() || email == null || email.isBlank()) {
            log.warn("Skipping token expired email — SMTP or recipient not configured");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("LinkedIn connection expired — action required");
            message.setText("""
                    Hi %s,

                    Your LinkedIn connection has expired. Please reconnect your account to resume automated posting.

                    Reconnect: %s/settings

                    — LinkedIn AI Agent
                    """.formatted(nameOrDefault(fullName), appProperties.getFrontendUrl()));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send LinkedIn token expired email to {}", email, e);
            throw new AgentException("Failed to send notification email", e);
        }
    }

    public void sendManualPublishApproval(String email, String fullName, String approvalUrl) {
        if (!isMailConfigured() || email == null || email.isBlank()) {
            log.warn("Skipping manual approval email — SMTP or recipient not configured");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Approve your LinkedIn post");
            message.setText("""
                    Hi %s,

                    Your LinkedIn post is ready to publish. Please approve within 2 hours:

                    %s

                    If you do not approve in time, the post will be skipped.

                    — LinkedIn AI Agent
                    """.formatted(nameOrDefault(fullName), approvalUrl));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send manual approval email to {}", email, e);
            throw new AgentException("Failed to send approval email", e);
        }
    }

    private boolean isMailConfigured() {
        return mailUsername != null && !mailUsername.isBlank();
    }

    private String nameOrDefault(String fullName) {
        return fullName != null && !fullName.isBlank() ? fullName : "there";
    }
}
