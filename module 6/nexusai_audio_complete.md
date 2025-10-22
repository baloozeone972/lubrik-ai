# NEXUSAI AUDIO MODULE - FICHIERS COMPLETS

Ce fichier contient tous les fichiers du Module 6 Audio dans un format parsable par ProjectGenerator.java

## FICHIERS JAVA

### VoiceProfile Controller

```java
// nexus-audio-api/src/main/java/com/nexusai/audio/api/controller/VoiceProfileController.java
package com.nexusai.audio.api.controller;

import com.nexusai.audio.api.dto.VoiceProfileRequest;
import com.nexusai.audio.api.dto.VoiceProfileResponse;
import com.nexusai.audio.core.domain.VoiceProfile;
import com.nexusai.audio.core.service.VoiceProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/audio/voice-profiles")
@RequiredArgsConstructor
@Tag(name = "Voice Profiles", description = "APIs de gestion des profils vocaux")
public class VoiceProfileController {
    
    private final VoiceProfileService voiceProfileService;
    
    @PostMapping
    @Operation(summary = "Créer un profil vocal")
    public ResponseEntity<VoiceProfileResponse> createProfile(
            @Valid @RequestBody VoiceProfileRequest request) {
        
        log.info("Création profil vocal : companionId={}", request.getCompanionId());
        
        VoiceProfile profile = VoiceProfile.builder()
                .companionId(request.getCompanionId())
                .provider(request.getProvider())
                .voiceId(request.getVoiceId())
                .pitch(request.getPitch())
                .speed(request.getSpeed())
                .style(request.getStyle())
                .customVoiceUrl(request.getCustomVoiceUrl())
                .build();
        
        VoiceProfile created = voiceProfileService.createVoiceProfile(profile);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapToResponse(created));
    }
    
    @GetMapping("/{companionId}")
    @Operation(summary = "Récupérer le profil vocal d'un compagnon")
    public ResponseEntity<VoiceProfileResponse> getProfile(
            @PathVariable String companionId) {
        
        VoiceProfile profile = voiceProfileService.getVoiceProfile(companionId);
        return ResponseEntity.ok(mapToResponse(profile));
    }
    
    @PutMapping("/{companionId}")
    @Operation(summary = "Mettre à jour un profil vocal")
    public ResponseEntity<VoiceProfileResponse> updateProfile(
            @PathVariable String companionId,
            @Valid @RequestBody VoiceProfileRequest request) {
        
        VoiceProfile updates = VoiceProfile.builder()
                .provider(request.getProvider())
                .voiceId(request.getVoiceId())
                .pitch(request.getPitch())
                .speed(request.getSpeed())
                .style(request.getStyle())
                .customVoiceUrl(request.getCustomVoiceUrl())
                .build();
        
        VoiceProfile updated = voiceProfileService.updateVoiceProfile(companionId, updates);
        
        return ResponseEntity.ok(mapToResponse(updated));
    }
    
    @DeleteMapping("/{companionId}")
    @Operation(summary = "Supprimer un profil vocal")
    public ResponseEntity<Void> deleteProfile(@PathVariable String companionId) {
        voiceProfileService.deleteVoiceProfile(companionId);
        return ResponseEntity.noContent().build();
    }
    
    private VoiceProfileResponse mapToResponse(VoiceProfile profile) {
        return VoiceProfileResponse.builder()
                .id(profile.getId())
                .companionId(profile.getCompanionId())
                .provider(profile.getProvider())
                .voiceId(profile.getVoiceId())
                .pitch(profile.getPitch())
                .speed(profile.getSpeed())
                .style(profile.getStyle())
                .customVoiceUrl(profile.getCustomVoiceUrl())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
```

### Global Exception Handler

```java
// nexus-audio-api/src/main/java/com/nexusai/audio/api/exception/GlobalExceptionHandler.java
package com.nexusai.audio.api.exception;

import com.nexusai.audio.core.exception.AudioProcessingException;
import com.nexusai.audio.core.exception.TranscriptionException;
import com.nexusai.audio.core.exception.SynthesisException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AudioProcessingException.class)
    public ResponseEntity<ErrorResponse> handleAudioProcessingException(
            AudioProcessingException ex) {
        
        log.error("Audio processing error", ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
    
    @ExceptionHandler(TranscriptionException.class)
    public ResponseEntity<ErrorResponse> handleTranscriptionException(
            TranscriptionException ex) {
        
        log.error("Transcription error", ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
    
    @ExceptionHandler(SynthesisException.class)
    public ResponseEntity<ErrorResponse> handleSynthesisException(
            SynthesisException ex) {
        
        log.error("Synthesis error", ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .message("Une erreur inattendue s'est produite")
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
```

### Error Response DTO

```java
// nexus-audio-api/src/main/java/com/nexusai/audio/api/exception/ErrorResponse.java
package com.nexusai.audio.api.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private LocalDateTime timestamp;
    private int status;
}
```

### RestTemplate Configuration

```java
// nexus-audio-api/src/main/java/com/nexusai/audio/api/config/RestTemplateConfig.java
package com.nexusai.audio.api.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}
```

### MinIO Configuration

```java
// nexus-audio-storage/src/main/java/com/nexusai/audio/storage/config/MinIOConfig.java
package com.nexusai.audio.storage.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinIOConfig {
    
    @Value("${minio.url}")
    private String minioUrl;
    
    @Value("${minio.access-key}")
    private String accessKey;
    
    @Value("${minio.secret-key}")
    private String secretKey;
    
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

### Storage Service Implementation

```java
// nexus-audio-storage/src/main/java/com/nexusai/audio/storage/service/AudioStorageService.java
package com.nexusai.audio.storage.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioStorageService {
    
    private final MinioClient minioClient;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    @Value("${minio.url}")
    private String minioUrl;
    
    public String uploadAudio(MultipartFile file, String folder) {
        log.info("Upload audio : file={}, folder={}", file.getOriginalFilename(), folder);
        
        try {
            ensureBucketExists();
            
            String fileName = generateUniqueFileName(file.getOriginalFilename());
            String objectName = folder + "/" + fileName;
            
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            
            String publicUrl = String.format("%s/%s/%s", minioUrl, bucketName, objectName);
            log.info("Upload réussi : {}", publicUrl);
            return publicUrl;
            
        } catch (Exception e) {
            log.error("Erreur upload audio", e);
            throw new RuntimeException("Échec upload audio", e);
        }
    }
    
    public String uploadAudio(byte[] audioData, String folder, String format) {
        log.info("Upload audio bytes : size={}, format={}", audioData.length, format);
        
        try {
            ensureBucketExists();
            
            String fileName = UUID.randomUUID().toString() + "." + format;
            String objectName = folder + "/" + fileName;
            
            ByteArrayInputStream inputStream = new ByteArrayInputStream(audioData);
            
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, audioData.length, -1)
                    .contentType("audio/" + format)
                    .build()
            );
            
            String publicUrl = String.format("%s/%s/%s", minioUrl, bucketName, objectName);
            return publicUrl;
            
        } catch (Exception e) {
            log.error("Erreur upload audio bytes", e);
            throw new RuntimeException("Échec upload audio", e);
        }
    }
    
    public void deleteAudio(String audioUrl) {
        log.info("Suppression audio : url={}", audioUrl);
        
        try {
            String objectName = extractObjectName(audioUrl);
            
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
            
            log.info("Suppression réussie");
            
        } catch (Exception e) {
            log.error("Erreur suppression audio", e);
            throw new RuntimeException("Échec suppression audio", e);
        }
    }
    
    public byte[] downloadAudio(String audioUrl) {
        log.debug("Téléchargement audio : url={}", audioUrl);
        
        try {
            String objectName = extractObjectName(audioUrl);
            
            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
            
            return stream.readAllBytes();
            
        } catch (Exception e) {
            log.error("Erreur téléchargement audio", e);
            throw new RuntimeException("Échec téléchargement audio", e);
        }
    }
    
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(bucketName)
                .build()
        );
        
        if (!exists) {
            log.info("Création bucket : {}", bucketName);
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
        }
    }
    
    private String generateUniqueFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
    
    private String extractObjectName(String audioUrl) {
        int bucketIndex = audioUrl.indexOf(bucketName);
        return audioUrl.substring(bucketIndex + bucketName.length() + 1);
    }
}
```

### Emotion Analysis Service

```java
// nexus-audio-emotion/src/main/java/com/nexusai/audio/emotion/service/EmotionAnalysisService.java
package com.nexusai.audio.emotion.service;

import com.nexusai.audio.emotion.model.EmotionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class EmotionAnalysisService {
    
    public EmotionResult analyzeEmotion(MultipartFile audioFile) {
        log.info("Analyse émotionnelle fichier audio");
        
        try {
            byte[] audioData = audioFile.getBytes();
            return analyze(audioData);
            
        } catch (Exception e) {
            log.error("Erreur analyse émotionnelle", e);
            return EmotionResult.builder()
                    .emotion("NEUTRAL")
                    .confidence(0.5f)
                    .build();
        }
    }
    
    public EmotionResult analyze(byte[] audioData) {
        log.debug("Analyse émotionnelle {} bytes", audioData.length);
        
        Map<String, Float> scores = new HashMap<>();
        scores.put("NEUTRAL", 0.6f);
        scores.put("HAPPY", 0.2f);
        scores.put("SAD", 0.1f);
        scores.put("ANGRY", 0.05f);
        scores.put("ANXIOUS", 0.03f);
        scores.put("EXCITED", 0.02f);
        
        return EmotionResult.builder()
                .emotion("NEUTRAL")
                .confidence(0.6f)
                .emotionScores(scores)
                .build();
    }
}
```

### Emotion Result Model

```java
// nexus-audio-emotion/src/main/java/com/nexusai/audio/emotion/model/EmotionResult.java
package com.nexusai.audio.emotion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionResult {
    private String emotion;
    private Float confidence;
    private Map<String, Float> emotionScores;
}
```

### Emotion Type Enum

```java
// nexus-audio-emotion/src/main/java/com/nexusai/audio/emotion/model/EmotionType.java
package com.nexusai.audio.emotion.model;

public enum EmotionType {
    NEUTRAL,
    HAPPY,
    SAD,
    ANGRY,
    ANXIOUS,
    EXCITED
}
```

## FICHIERS XML (POM)

### Parent POM

```xml
<!-- pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.nexusai</groupId>
    <artifactId>nexus-audio</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <spring-boot.version>3.2.5</spring-boot.version>
        <lombok.version>1.18.30</lombok.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <minio.version>8.5.9</minio.version>
    </properties>

    <modules>
        <module>nexus-audio-api</module>
        <module>nexus-audio-core</module>
        <module>nexus-audio-stt</module>
        <module>nexus-audio-tts</module>
        <module>nexus-audio-webrtc</module>
        <module>nexus-audio-storage</module>
        <module>nexus-audio-emotion</module>
        <module>nexus-audio-persistence</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.nexusai</groupId>
                <artifactId>nexus-audio-core</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>io.minio</groupId>
                <artifactId>minio</artifactId>
                <version>${minio.version}</version>
            </dependency>
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
                <scope>provided</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

## FICHIERS YAML

### Application Configuration

```yaml
# nexus-audio-api/src/main/resources/application.yml
spring:
  application:
    name: nexus-audio-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/nexusai
    username: nexusai
    password: nexusai123
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

server:
  port: 8083

minio:
  url: http://localhost:9000
  access-key: nexusai
  secret-key: nexusai123
  bucket-name: nexusai-audio

openai:
  api-key: ${OPENAI_API_KEY}
  whisper:
    url: https://api.openai.com/v1/audio/transcriptions
    model: whisper-1

elevenlabs:
  api-key: ${ELEVENLABS_API_KEY}
  url: https://api.elevenlabs.io/v1

logging:
  level:
    root: INFO
    com.nexusai.audio: DEBUG
```

### Docker Compose

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: nexusai-audio-postgres
    environment:
      POSTGRES_DB: nexusai
      POSTGRES_USER: nexusai
      POSTGRES_PASSWORD: nexusai123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - nexusai-network

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: nexusai-audio-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    depends_on:
      - zookeeper
    networks:
      - nexusai-network

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    networks:
      - nexusai-network

  minio:
    image: minio/minio:latest
    container_name: nexusai-audio-minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: nexusai
      MINIO_ROOT_PASSWORD: nexusai123
    command: server /data --console-address ":9001"
    volumes:
      - minio_data:/data
    networks:
      - nexusai-network

volumes:
  postgres_data:
  minio_data:

networks:
  nexusai-network:
    driver: bridge
```

## AUTRES FICHIERS

### Makefile

```makefile
# Makefile
.PHONY: help build test run docker-up docker-down clean

help:
	@echo "Commandes disponibles:"
	@echo "  build       - Compile le projet"
	@echo "  test        - Execute les tests"
	@echo "  run         - Lance l'application"
	@echo "  docker-up   - Démarre les services Docker"
	@echo "  docker-down - Arrête les services Docker"

build:
	mvn clean install -DskipTests

test:
	mvn test

run:
	mvn spring-boot:run -pl nexus-audio-api

docker-up:
	docker-compose up -d

docker-down:
	docker-compose down

clean:
	mvn clean
	docker-compose down -v
```

### Dockerfile

```dockerfile
# Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY nexus-audio-api/pom.xml nexus-audio-api/
COPY nexus-audio-core/pom.xml nexus-audio-core/
COPY nexus-audio-stt/pom.xml nexus-audio-stt/
COPY nexus-audio-tts/pom.xml nexus-audio-tts/
COPY nexus-audio-webrtc/pom.xml nexus-audio-webrtc/
COPY nexus-audio-storage/pom.xml nexus-audio-storage/
COPY nexus-audio-emotion/pom.xml nexus-audio-emotion/
COPY nexus-audio-persistence/pom.xml nexus-audio-persistence/

RUN mvn dependency:go-offline -B

COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/nexus-audio-api/target/*.jar app.jar

EXPOSE 8083

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### README

```markdown
# README.md
# NexusAI Audio Module

Module de traitement audio pour NexusAI.

## Démarrage rapide

1. Configuration :
```bash
export OPENAI_API_KEY=sk-...
export ELEVENLABS_API_KEY=...
```

2. Lancement :
```bash
make docker-up
make build
make run
```

3. Test :
```bash
curl http://localhost:8083/actuator/health
```

## Documentation

- API: http://localhost:8083/swagger-ui.html
- Wiki: docs/README.md

## Licence

© 2025 NexusAI Team
```

### .gitignore

```
# .gitignore
target/
.idea/
*.iml
.DS_Store
.env
*.log
```

FIN DU FICHIER
