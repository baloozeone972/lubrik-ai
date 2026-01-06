package com.nexusai.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration Spring pour le module AI Engine.
 */
@Configuration
public class AIConfig {
    
    /**
     * Bean WebClient.Builder pour les clients HTTP réactifs.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
