package com.linkedinagent.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIConfig {
    // Spring AI auto-configures GoogleAiGeminiChatModel via spring.ai.google.genai.* properties.
    // GeminiRateLimiter wraps all agent calls with RPM + daily budget guards.
}
