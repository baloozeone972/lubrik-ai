/**
 * ═══════════════════════════════════════════════════════════════════════════
 * MODULE 5 : MEDIA GENERATION (IMAGES) - NEXUSAI
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Architecture multi-module Maven pour la génération d'images via IA
 * 
 * STRUCTURE DU PROJET:
 * 
 * nexus-image-generation/
 * ├── pom.xml (Parent POM)
 * ├── nexus-image-api/              (REST API & Controllers)
 * ├── nexus-image-core/             (Business Logic & Services)
 * ├── nexus-image-domain/           (Entités & DTOs)
 * ├── nexus-image-infrastructure/   (Repositories & External APIs)
 * ├── nexus-image-worker/           (Worker Python pour génération)
 * └── nexus-image-config/           (Configuration commune)
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════════════════
// 1. PARENT POM.XML
// ═══════════════════════════════════════════════════════════════════════════

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.nexusai</groupId>
    <artifactId>nexus-image-generation</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>NexusAI Image Generation Module</name>
    <description>Module de génération d'images via IA</description>

    <modules>
        <module>nexus-image-domain</module>
        <module>nexus-image-infrastructure</module>
        <module>nexus-image-core</module>
        <module>nexus-image-api</module>
        <module>nexus-image-config</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Dependencies versions -->
        <lombok.version>1.18.30</lombok.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <springdoc.version>2.2.0</springdoc.version>
        <kafka.version>3.6.0</kafka.version>
        <aws.sdk.version>2.21.0</aws.sdk.version>
        <testcontainers.version>1.19.0</testcontainers.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-data-jpa</artifactId>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-validation</artifactId>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-actuator</artifactId>
            </dependency>
            
            <!-- Kafka -->
            <dependency>
                <groupId>org.springframework.kafka</groupId>
                <artifactId>spring-kafka</artifactId>
            </dependency>
            
            <!-- PostgreSQL -->
            <dependency>
                <groupId>org.postgresql</groupId>
                <artifactId>postgresql</artifactId>
                <scope>runtime</scope>
            </dependency>
            
            <!-- AWS SDK -->
            <dependency>
                <groupId>software.amazon.awssdk</groupId>
                <artifactId>s3</artifactId>
                <version>${aws.sdk.version}</version>
            </dependency>
            
            <!-- Lombok -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
                <scope>provided</scope>
            </dependency>
            
            <!-- MapStruct -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
            
            <!-- OpenAPI Documentation -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>
            
            <!-- Test -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-test</artifactId>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers</artifactId>
                <version>${testcontainers.version}</version>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>postgresql</artifactId>
                <version>${testcontainers.version}</version>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>kafka</artifactId>
                <version>${testcontainers.version}</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>

// ═══════════════════════════════════════════════════════════════════════════
// 2. MODULE DOMAIN - ENTITÉS & DTOS
// ═══════════════════════════════════════════════════════════════════════════

package com.nexusai.image.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant une image générée
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01-01
 */
@Entity
@Table(name = "generated_images", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "companion_id")
    private String companionId;

    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "negative_prompt", columnDefinition = "TEXT")
    private String negativePrompt;

    @Column(name = "style", length = 50)
    private String style;

    @Column(name = "resolution", length = 20)
    private String resolution;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ImageStatus status = ImageStatus.QUEUED;

    @Column(name = "storage_url", columnDefinition = "TEXT")
    private String storageUrl;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "seed")
    private Integer seed;

    @Column(name = "parameters", columnDefinition = "JSONB")
    private String parameters;

    @Column(name = "generation_time_seconds")
    private Integer generationTimeSeconds;

    @Column(name = "is_favorite")
    private Boolean isFavorite = false;

    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Column(name = "tokens_cost", nullable = false)
    private Integer tokensCost;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Énumération des statuts possibles d'une image
     */
    public enum ImageStatus {
        QUEUED,      // En attente dans la file
        PROCESSING,  // En cours de génération
        COMPLETED,   // Génération terminée avec succès
        FAILED       // Échec de la génération
    }
}

// ═══════════════════════════════════════════════════════════════════════════

package com.nexusai.image.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO pour la requête de génération d'image
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageGenerationRequest {

    @NotBlank(message = "Le prompt est obligatoire")
    @Size(min = 10, max = 1000, message = "Le prompt doit contenir entre 10 et 1000 caractères")
    private String prompt;

    @Size(max = 500, message = "Le negative prompt ne peut pas dépasser 500 caractères")
    @JsonProperty("negative_prompt")
    private String negativePrompt;

    @Pattern(regexp = "realistic|anime|artistic|3d|sketch", 
             message = "Style invalide")
    private String style = "realistic";

    @Pattern(regexp = "512x512|768x768|1024x1024|1024x1536", 
             message = "Résolution invalide")
    private String resolution = "1024x1024";

    @JsonProperty("companion_id")
    private String companionId;

    @Min(value = 0, message = "Le seed doit être positif")
    @Max(value = 2147483647, message = "Le seed est trop grand")
    private Integer seed;

    @JsonProperty("is_public")
    private Boolean isPublic = false;
}
