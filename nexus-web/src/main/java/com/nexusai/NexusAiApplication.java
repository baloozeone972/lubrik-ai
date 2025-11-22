package com.nexusai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * NexusAI - Advanced AI Companion Platform
 *
 * Main Spring Boot Application Entry Point.
 * This application provides a comprehensive AI companion experience
 * with multi-modal interactions, emotion recognition, and personalization.
 */
@SpringBootApplication(scanBasePackages = "com.nexusai")
@EntityScan(basePackages = "com.nexusai")
@EnableJpaRepositories(basePackages = "com.nexusai")
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class NexusAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusAiApplication.class, args);
    }
}
