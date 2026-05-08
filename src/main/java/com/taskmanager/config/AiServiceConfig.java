package com.taskmanager.config;

import com.taskmanager.service.ai.AiSuggestionService;
import com.taskmanager.service.ai.GeminiSuggestionService;
import com.taskmanager.service.ai.MockAiSuggestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the AI implementation at startup based on the GEMINI_API_KEY
 * environment variable (or Spring property of the same name).
 *
 * - Key present  → GeminiSuggestionService  (real API call)
 * - Key absent   → MockAiSuggestionService  (deterministic keyword-based fallback)
 *
 * The key is injected via @Value so Spring manages it; it is never hardcoded
 * and never logged beyond the first character.
 */
@Configuration
public class AiServiceConfig {

    private static final Logger log = LoggerFactory.getLogger(AiServiceConfig.class);

    @Bean
    public AiSuggestionService aiSuggestionService(
            @Value("${GEMINI_API_KEY:}") String apiKey) {

        if (apiKey != null && !apiKey.isBlank()) {
            log.info("AI provider: Gemini (key configured, starts with '{}')", apiKey.charAt(0));
            return new GeminiSuggestionService(apiKey);
        }

        log.info("AI provider: Mock (GEMINI_API_KEY not set – using deterministic fallback)");
        return new MockAiSuggestionService();
    }
}
