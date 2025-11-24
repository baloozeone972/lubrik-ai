===========================================
MODULE 6 : AUDIO PROCESSING
NEXUSAI - Architecture Modulaire
===========================================

STRUCTURE DU PROJET MAVEN MULTI-MODULE
=======================================

nexus-audio/
├── pom.xml (Parent POM)
├── README.md
├── docker-compose.yml
│
├── nexus-audio-api/              (Module 1 - API REST)
│   ├── pom.xml
│   └── src/main/java/com/nexusai/audio/api/
│       ├── AudioApplication.java
│       ├── controller/
│       │   ├── VoiceMessageController.java
│       │   ├── VoiceCallController.java
│       │   └── VoiceProfileController.java
│       ├── dto/
│       │   ├── VoiceMessageRequest.java
│       │   ├── VoiceMessageResponse.java
│       │   ├── TranscriptionRequest.java
│       │   ├── SynthesisRequest.java
│       │   └── VoiceCallRequest.java
│       └── config/
│           ├── SecurityConfig.java
│           └── WebSocketConfig.java
│
├── nexus-audio-core/             (Module 2 - Logique Métier)
│   ├── pom.xml
│   └── src/main/java/com/nexusai/audio/core/
│       ├── service/
│       │   ├── VoiceMessageService.java
│       │   ├── VoiceCallService.java
│       │   ├── VoiceProfileService.java
│       │   └── EmotionDetectionService.java
│       ├── domain/
│       │   ├── VoiceMessage.java
│       │   ├── VoiceCall.java
│       │   └── VoiceProfile.java
│       └── exception/
│           ├── AudioProcessingException.java
│           └── TranscriptionException.java
│
├── nexus-audio-stt/              (Module 3 - Speech-to-Text)
│   ├── pom.xml
│   └── src/main/java/com/nexusai/audio/stt/
│       ├── service/
│       │   ├── WhisperSTTService.java
│       │   └── STTServiceFactory.java
│       ├── client/
│       │   └── OpenAIWhisperClient.java
│       └── model/
│           └── TranscriptionResult.java
│
├── nexus-audio-tts/              (Module 4 - Text-to-Speech)
│   ├── pom.xml
│   └── src/main/java/com/nexusai/audio/tts/
│       ├── service/
│       │   ├── ElevenLabsTTSService.java
│       │   ├── CoquiTTSService.java
│       │   └── TTSServiceFactory.java
│       ├── client/
│       │   └── ElevenLabsClient.java
│       └── model/
│           ├── VoiceSettings.java
│           └── SynthesisResult.java
│
├── nexus-audio-webrtc/           (Module 5 - WebRTC)
│   ├── pom.xml
│   └── src/main/java/com/nexusai/audio/webrtc/
│       ├── service/
│       │   ├── WebRTCSessionService.java
│       │   └── JanusGatewayService.java
│       ├── handler/
│       │   └── WebRTCSignalingHandler.java
│       └── model/
│           ├── WebRTCSession.java
│           └── SignalingMessage.java
│
├── nexus-audio-storage/          (Module 6 - Stockage S3/MinIO)
│   ├── pom.xml
│   └── src/main/java/com/nexusai/audio/storage/
│       ├── service/
│       │   ├── AudioStorageService.java
│       │   └── S3StorageService.java
│       └── config/
│           └── MinIOConfig.java
│
├── nexus-audio-emotion/          (Module 7 - Analyse Émotionnelle)
│   ├── pom.xml
│   └── src/main/java/com/nexusai/audio/emotion/
│       ├── service/
│       │   └── EmotionAnalysisService.java
│       ├── model/
│       │   ├── EmotionResult.java
│       │   └── EmotionType.java
│       └── ml/
│           └── EmotionModelLoader.java
│
└── nexus-audio-persistence/      (Module 8 - Base de Données)
    ├── pom.xml
    └── src/main/java/com/nexusai/audio/persistence/
        ├── entity/
        │   ├── VoiceMessageEntity.java
        │   ├── VoiceCallEntity.java
        │   └── VoiceProfileEntity.java
        ├── repository/
        │   ├── VoiceMessageRepository.java
        │   ├── VoiceCallRepository.java
        │   └── VoiceProfileRepository.java
        └── mapper/
            ├── VoiceMessageMapper.java
            └── VoiceProfileMapper.java

===========================================
PARENT POM.XML
===========================================

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

    <name>NexusAI Audio Processing Module</name>
    <description>Module de traitement audio pour NexusAI</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Versions des dépendances -->
        <spring-boot.version>3.2.5</spring-boot.version>
        <lombok.version>1.18.30</lombok.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <minio.version>8.5.9</minio.version>
        <kafka.version>3.6.0</kafka.version>
        <jackson.version>2.15.3</jackson.version>
        <openapi.version>2.3.0</openapi.version>
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
            <!-- Modules internes -->
            <dependency>
                <groupId>com.nexusai</groupId>
                <artifactId>nexus-audio-core</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.nexusai</groupId>
                <artifactId>nexus-audio-stt</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.nexusai</groupId>
                <artifactId>nexus-audio-tts</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.nexusai</groupId>
                <artifactId>nexus-audio-storage</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.nexusai</groupId>
                <artifactId>nexus-audio-persistence</artifactId>
                <version>${project.version}</version>
            </dependency>

            <!-- Spring Boot -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
                <version>${spring-boot.version}</version>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-data-jpa</artifactId>
                <version>${spring-boot.version}</version>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-websocket</artifactId>
                <version>${spring-boot.version}</version>
            </dependency>

            <!-- PostgreSQL -->
            <dependency>
                <groupId>org.postgresql</groupId>
                <artifactId>postgresql</artifactId>
                <version>42.7.1</version>
            </dependency>

            <!-- Kafka -->
            <dependency>
                <groupId>org.springframework.kafka</groupId>
                <artifactId>spring-kafka</artifactId>
                <version>${kafka.version}</version>
            </dependency>

            <!-- MinIO -->
            <dependency>
                <groupId>io.minio</groupId>
                <artifactId>minio</artifactId>
                <version>${minio.version}</version>
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

            <!-- OpenAPI/Swagger -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${openapi.version}</version>
            </dependency>

            <!-- Tests -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-test</artifactId>
                <version>${spring-boot.version}</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <configuration>
                        <excludes>
                            <exclude>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </exclude>
                        </excludes>
                    </configuration>
                </plugin>
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
        </pluginManagement>
    </build>
</project>

===========================================
RÉPARTITION DES TÂCHES PAR DÉVELOPPEUR
===========================================

DÉVELOPPEUR 1 : API & Configuration
------------------------------------
- nexus-audio-api (Module 1)
  * Controllers REST
  * DTOs
  * Configuration WebSocket
  * Documentation OpenAPI

Estimation : 2 semaines

DÉVELOPPEUR 2 : Logique Métier & Persistence
--------------------------------------------
- nexus-audio-core (Module 2)
- nexus-audio-persistence (Module 8)
  * Services métier
  * Entités JPA
  * Repositories
  * Mappers

Estimation : 2 semaines

DÉVELOPPEUR 3 : Speech-to-Text
--------------------------------
- nexus-audio-stt (Module 3)
  * Intégration OpenAI Whisper
  * Service de transcription
  * Gestion des formats audio

Estimation : 1.5 semaines

DÉVELOPPEUR 4 : Text-to-Speech
-------------------------------
- nexus-audio-tts (Module 4)
  * Intégration ElevenLabs
  * Service de synthèse vocale
  * Personnalisation voix

Estimation : 1.5 semaines

DÉVELOPPEUR 5 : WebRTC & Temps Réel
------------------------------------
- nexus-audio-webrtc (Module 5)
  * Gestion sessions WebRTC
  * Signaling
  * Intégration Janus

Estimation : 2.5 semaines

DÉVELOPPEUR 6 : Stockage & Analyse
-----------------------------------
- nexus-audio-storage (Module 6)
- nexus-audio-emotion (Module 7)
  * Intégration S3/MinIO
  * Upload/Download fichiers
  * Analyse émotionnelle

Estimation : 2 semaines

===========================================
FICHIERS DE CONFIGURATION
===========================================

Voir les artifacts suivants pour :
- Code source détaillé de chaque module
- Configuration Spring Boot
- Docker Compose
- Documentation API
- Tests unitaires

===========================================
DÉPENDANCES INTER-MODULES
===========================================

nexus-audio-api
    ├── depends on → nexus-audio-core
    ├── depends on → nexus-audio-stt
    ├── depends on → nexus-audio-tts
    ├── depends on → nexus-audio-webrtc
    └── depends on → nexus-audio-storage

nexus-audio-core
    ├── depends on → nexus-audio-persistence
    ├── depends on → nexus-audio-stt
    ├── depends on → nexus-audio-tts
    └── depends on → nexus-audio-emotion

nexus-audio-stt → (pas de dépendances internes)
nexus-audio-tts → (pas de dépendances internes)
nexus-audio-webrtc → nexus-audio-core
nexus-audio-storage → (pas de dépendances internes)
nexus-audio-emotion → (pas de dépendances internes)
nexus-audio-persistence → (pas de dépendances internes)

Cette architecture permet à chaque développeur de travailler
indépendamment sur son module avec des interfaces claires
et bien définies.
