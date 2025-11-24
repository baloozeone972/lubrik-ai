// ===========================================
// MODULE 6.8 : AUDIO PERSISTENCE
// ===========================================

// pom.xml pour nexus-audio-persistence
// ---------------------------------------------
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.nexusai</groupId>
        <artifactId>nexus-audio</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>nexus-audio-persistence</artifactId>
    <packaging>jar</packaging>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        <dependency>
            <groupId>io.hypersistence</groupId>
            <artifactId>hypersistence-utils-hibernate-63</artifactId>
            <version>3.7.0</version>
        </dependency>
    </dependencies>
</project>

// ===========================================
// ENTITÉS JPA
// ===========================================

package com.nexusai.audio.persistence.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité JPA représentant un message vocal en base de données.
 * 
 * <p>Table : voice_messages</p>
 * <p>Index :</p>
 * <ul>
 *   <li>idx_voice_messages_conv : sur conversation_id</li>
 *   <li>idx_voice_messages_user : sur user_id</li>
 *   <li>idx_voice_messages_created : sur created_at DESC</li>
 * </ul>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Entity
@Table(name = "voice_messages", indexes = {
    @Index(name = "idx_voice_messages_conv", columnList = "conversation_id"),
    @Index(name = "idx_voice_messages_user", columnList = "user_id"),
    @Index(name = "idx_voice_messages_created", columnList = "created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceMessageEntity {
    
    /**
     * Identifiant unique (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    /**
     * ID de la conversation.
     */
    @Column(name = "conversation_id", nullable = false, length = 255)
    private String conversationId;
    
    /**
     * ID de l'utilisateur.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    /**
     * ID du compagnon IA.
     */
    @Column(name = "companion_id", length = 255)
    private String companionId;
    
    /**
     * Type d'émetteur (USER ou COMPANION).
     */
    @Column(name = "sender", nullable = false, length = 20)
    private String sender;
    
    /**
     * URL de stockage du fichier audio.
     */
    @Column(name = "audio_url", nullable = false, columnDefinition = "TEXT")
    private String audioUrl;
    
    /**
     * Durée de l'enregistrement en secondes.
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    /**
     * Taille du fichier en octets.
     */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    
    /**
     * Transcription textuelle du message.
     */
    @Column(name = "transcription", columnDefinition = "TEXT")
    private String transcription;
    
    /**
     * Langue détectée (code ISO 639-1).
     */
    @Column(name = "transcription_language", length = 10)
    private String transcriptionLanguage;
    
    /**
     * Émotion détectée.
     */
    @Column(name = "emotion_detected", length = 50)
    private String emotionDetected;
    
    /**
     * Score de confiance de la détection émotionnelle.
     */
    @Column(name = "emotion_confidence")
    private Float emotionConfidence;
    
    /**
     * Date de création.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * Hook pré-persist pour initialiser created_at.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

package com.nexusai.audio.persistence.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entité JPA représentant un appel vocal en base de données.
 * 
 * <p>Table : voice_calls</p>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Entity
@Table(name = "voice_calls", indexes = {
    @Index(name = "idx_voice_calls_user", columnList = "user_id"),
    @Index(name = "idx_voice_calls_status", columnList = "status"),
    @Index(name = "idx_voice_calls_created", columnList = "created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceCallEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "conversation_id", nullable = false, length = 255)
    private String conversationId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "companion_id", length = 255)
    private String companionId;
    
    @Column(name = "status", length = 20)
    private String status;
    
    @Column(name = "webrtc_session_id", length = 255)
    private String webrtcSessionId;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    /**
     * Métriques de qualité stockées en JSON.
     */
    @Type(JsonBinaryType.class)
    @Column(name = "quality_metrics", columnDefinition = "jsonb")
    private Map<String, Object> qualityMetrics;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "INITIATED";
        }
    }
}

package com.nexusai.audio.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité JPA représentant un profil vocal en base de données.
 * 
 * <p>Table : voice_profiles</p>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Entity
@Table(name = "voice_profiles", indexes = {
    @Index(name = "idx_voice_profiles_companion", columnList = "companion_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceProfileEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    /**
     * ID du compagnon (unique).
     */
    @Column(name = "companion_id", unique = true, nullable = false, length = 255)
    private String companionId;
    
    /**
     * Fournisseur TTS (ELEVENLABS, COQUI, etc.).
     */
    @Column(name = "provider", length = 50)
    private String provider;
    
    /**
     * ID de la voix chez le fournisseur.
     */
    @Column(name = "voice_id", length = 255)
    private String voiceId;
    
    /**
     * Hauteur tonale (-1.0 à 1.0).
     */
    @Column(name = "pitch")
    private Float pitch;
    
    /**
     * Vitesse d'élocution (0.5 à 2.0).
     */
    @Column(name = "speed")
    private Float speed;
    
    /**
     * Style de voix.
     */
    @Column(name = "style", length = 50)
    private String style;
    
    /**
     * URL d'une voix personnalisée.
     */
    @Column(name = "custom_voice_url", columnDefinition = "TEXT")
    private String customVoiceUrl;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

// ===========================================
// REPOSITORIES JPA
// ===========================================

package com.nexusai.audio.persistence.repository;

import com.nexusai.audio.persistence.entity.VoiceMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository JPA pour l'entité VoiceMessageEntity.
 * 
 * <p>Fournit les opérations CRUD de base ainsi que des requêtes
 * personnalisées pour les messages vocaux.</p>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Repository
public interface VoiceMessageRepository extends JpaRepository<VoiceMessageEntity, UUID> {
    
    /**
     * Trouve tous les messages vocaux d'une conversation, triés par date.
     * 
     * @param conversationId ID de la conversation
     * @return Liste des messages vocaux triée par date croissante
     */
    List<VoiceMessageEntity> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    
    /**
     * Trouve tous les messages vocaux d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Liste des messages vocaux
     */
    List<VoiceMessageEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Compte le nombre de messages vocaux dans une conversation.
     * 
     * @param conversationId ID de la conversation
     * @return Nombre de messages
     */
    long countByConversationId(String conversationId);
    
    /**
     * Supprime tous les messages vocaux d'une conversation.
     * 
     * @param conversationId ID de la conversation
     */
    void deleteByConversationId(String conversationId);
}

package com.nexusai.audio.persistence.repository;

import com.nexusai.audio.persistence.entity.VoiceCallEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour l'entité VoiceCallEntity.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Repository
public interface VoiceCallRepository extends JpaRepository<VoiceCallEntity, UUID> {
    
    /**
     * Trouve tous les appels d'un utilisateur, triés par date décroissante.
     * 
     * @param userId ID de l'utilisateur
     * @return Liste des appels
     */
    List<VoiceCallEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Trouve un appel actif (statut CONNECTED) pour un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @param status Statut recherché (ex: "CONNECTED")
     * @return L'appel actif s'il existe
     */
    Optional<VoiceCallEntity> findByUserIdAndStatus(UUID userId, String status);
    
    /**
     * Trouve tous les appels avec un statut donné.
     * 
     * @param status Statut recherché
     * @return Liste des appels
     */
    List<VoiceCallEntity> findByStatus(String status);
    
    /**
     * Compte le nombre total d'appels d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Nombre d'appels
     */
    long countByUserId(UUID userId);
}

package com.nexusai.audio.persistence.repository;

import com.nexusai.audio.persistence.entity.VoiceProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour l'entité VoiceProfileEntity.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Repository
public interface VoiceProfileRepository extends JpaRepository<VoiceProfileEntity, UUID> {
    
    /**
     * Trouve le profil vocal d'un compagnon.
     * 
     * @param companionId ID du compagnon
     * @return Le profil vocal s'il existe
     */
    Optional<VoiceProfileEntity> findByCompanionId(String companionId);
    
    /**
     * Vérifie si un compagnon possède un profil vocal.
     * 
     * @param companionId ID du compagnon
     * @return true si un profil existe
     */
    boolean existsByCompanionId(String companionId);
    
    /**
     * Supprime le profil vocal d'un compagnon.
     * 
     * @param companionId ID du compagnon
     */
    void deleteByCompanionId(String companionId);
}

// ===========================================
// MAPPERS MAPSTRUCT
// ===========================================

package com.nexusai.audio.persistence.mapper;

import com.nexusai.audio.core.domain.VoiceMessage;
import com.nexusai.audio.persistence.entity.VoiceMessageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct pour convertir entre VoiceMessage (domaine) et
 * VoiceMessageEntity (JPA).
 * 
 * <p>MapStruct génère automatiquement l'implémentation de ce mapper
 * lors de la compilation.</p>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VoiceMessageMapper {
    
    /**
     * Convertit un objet domaine en entité JPA.
     * 
     * @param voiceMessage Objet domaine
     * @return Entité JPA
     */
    @Mapping(target = "sender", expression = "java(voiceMessage.getSender().name())")
    VoiceMessageEntity toEntity(VoiceMessage voiceMessage);
    
    /**
     * Convertit une entité JPA en objet domaine.
     * 
     * @param entity Entité JPA
     * @return Objet domaine
     */
    @Mapping(target = "sender", 
            expression = "java(VoiceMessage.SenderType.valueOf(entity.getSender()))")
    VoiceMessage toDomain(VoiceMessageEntity entity);
}

package com.nexusai.audio.persistence.mapper;

import com.nexusai.audio.core.domain.VoiceCall;
import com.nexusai.audio.persistence.entity.VoiceCallEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct pour VoiceCall.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VoiceCallMapper {
    
    @Mapping(target = "status", expression = "java(voiceCall.getStatus().name())")
    VoiceCallEntity toEntity(VoiceCall voiceCall);
    
    @Mapping(target = "status", 
            expression = "java(VoiceCall.CallStatus.valueOf(entity.getStatus()))")
    VoiceCall toDomain(VoiceCallEntity entity);
}

package com.nexusai.audio.persistence.mapper;

import com.nexusai.audio.core.domain.VoiceProfile;
import com.nexusai.audio.persistence.entity.VoiceProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct pour VoiceProfile.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VoiceProfileMapper {
    
    VoiceProfileEntity toEntity(VoiceProfile voiceProfile);
    
    VoiceProfile toDomain(VoiceProfileEntity entity);
}

// ===========================================
// SCRIPT SQL DE CRÉATION DES TABLES
// ===========================================

/*
-- Fichier : src/main/resources/db/migration/V1__create_voice_tables.sql
-- Migration Flyway pour créer les tables audio

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Table des messages vocaux
CREATE TABLE voice_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    companion_id VARCHAR(255),
    sender VARCHAR(20) NOT NULL CHECK (sender IN ('USER', 'COMPANION')),
    audio_url TEXT NOT NULL,
    duration_seconds INTEGER,
    file_size_bytes BIGINT,
    transcription TEXT,
    transcription_language VARCHAR(10),
    emotion_detected VARCHAR(50),
    emotion_confidence FLOAT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_voice_messages_conv ON voice_messages(conversation_id);
CREATE INDEX idx_voice_messages_user ON voice_messages(user_id);
CREATE INDEX idx_voice_messages_created ON voice_messages(created_at DESC);

-- Table des appels vocaux
CREATE TABLE voice_calls (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    companion_id VARCHAR(255),
    status VARCHAR(20) DEFAULT 'INITIATED' CHECK (status IN ('INITIATED', 'RINGING', 'CONNECTED', 'ENDED', 'FAILED')),
    webrtc_session_id VARCHAR(255),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    duration_seconds INTEGER,
    quality_metrics JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_voice_calls_user ON voice_calls(user_id);
CREATE INDEX idx_voice_calls_status ON voice_calls(status);
CREATE INDEX idx_voice_calls_created ON voice_calls(created_at DESC);

-- Table des profils vocaux
CREATE TABLE voice_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    companion_id VARCHAR(255) UNIQUE NOT NULL,
    provider VARCHAR(50),
    voice_id VARCHAR(255),
    pitch FLOAT,
    speed FLOAT,
    style VARCHAR(50),
    custom_voice_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_voice_profiles_companion ON voice_profiles(companion_id);

-- Commentaires pour la documentation
COMMENT ON TABLE voice_messages IS 'Messages vocaux enregistrés par les utilisateurs et compagnons IA';
COMMENT ON TABLE voice_calls IS 'Appels vocaux en temps réel via WebRTC';
COMMENT ON TABLE voice_profiles IS 'Profils vocaux personnalisés des compagnons IA';
*/
