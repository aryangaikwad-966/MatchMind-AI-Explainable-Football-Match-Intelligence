package com.matchmind.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "ai.mode", havingValue = "GEMINI")
public class AIConfig {
    // Kept as a lightweight marker for Gemini mode; Gemini is called over its HTTPS API.
}
