package com.nexusai.core;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration class for testing.
 * This allows @DataJpaTest to find a Spring Boot configuration.
 */
@SpringBootApplication
@EntityScan(basePackages = "com.nexusai.core.entity")
@EnableJpaRepositories(basePackages = "com.nexusai.core.repository")
public class TestConfig {
}
