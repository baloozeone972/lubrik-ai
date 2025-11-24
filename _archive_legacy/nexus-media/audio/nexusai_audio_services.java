// ===========================================
// SERVICES VOCAUX COMPLÉMENTAIRES
// ===========================================

package com.nexusai.audio.core.service;

import com.nexusai.audio.core.domain.VoiceCall;
import com.nexusai.audio.core.exception.AudioProcessingException;
import com.nexusai.audio.persistence.entity.VoiceCallEntity;
import com.nexusai.audio.persistence.repository.VoiceCallRepository;
import com.nexusai.audio.persistence.mapper.VoiceCallMapper;
import com.nexusai.audio.webrtc.service.WebRTCSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de gestion des appels vocaux en temps réel.
 * 
 * <p>Ce service gère le cycle de vie complet des appels vocaux WebRTC
 * entre un utilisateur et son compagnon IA :</p>
 * <ul>
 *   <li>Initiation de l'appel</li>
 *   <li>Gestion de la connexion WebRTC</li>
 *   <li>Suivi de la qualité de l'appel</li>
 *   <li>Terminaison et statistiques</li>
 * </ul>
 * 
 * <p><strong>Workflow typique :</strong></p>
 * <pre>{@code
 * // 1. Initier l'appel
 * VoiceCall call = voiceCallService.initiateCall(userId, companionId, conversationId);
 * 
 * // 2. Établir la connexion WebRTC (côté client)
 * // ...
 * 
 * // 3. Mettre à jour le statut
 * voiceCallService.updateCallStatus(call.getId(), VoiceCall.CallStatus.CONNECTED);
 * 
 * // 4. Terminer l'appel
 * voiceCallService.endCall(call.getId(), qualityMetrics);
 * }</pre>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceCallService {
    
    private final VoiceCallRepository voiceCallRepository;
    private final VoiceCallMapper voiceCallMapper;
    private final WebRTCSessionService webRTCSessionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TOPIC_CALL_INITIATED = "voice.call.initiated";
    private static final String TOPIC_CALL_CONNECTED = "voice.call.connected";
    private static final String TOPIC_CALL_ENDED = "voice.call.ended";
    
    /**
     * Initie un nouvel appel vocal.
     * 
     * <p>Cette méthode crée une session d'appel et génère les informations
     * nécessaires pour établir la connexion WebRTC.</p>
     * 
     * @param userId ID de l'utilisateur appelant
     * @param companionId ID du compagnon IA
     * @param conversationId ID de la conversation
     * @return L'appel vocal créé avec status INITIATED
     * @throws AudioProcessingException Si l'initiation échoue
     */
    @Transactional
    public VoiceCall initiateCall(
            UUID userId,
            String companionId,
            String conversationId) {
        
        log.info("Initiation d'un appel vocal : userId={}, companionId={}", 
                userId, companionId);
        
        try {
            // 1. Créer la session WebRTC
            String webrtcSessionId = webRTCSessionService.createSession(
                    userId, 
                    companionId
            );
            
            // 2. Créer l'appel vocal
            VoiceCall voiceCall = VoiceCall.builder()
                    .id(UUID.randomUUID())
                    .conversationId(conversationId)
                    .userId(userId)
                    .companionId(companionId)
                    .status(VoiceCall.CallStatus.INITIATED)
                    .webrtcSessionId(webrtcSessionId)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            // 3. Sauvegarder en base
            VoiceCallEntity entity = voiceCallMapper.toEntity(voiceCall);
            entity = voiceCallRepository.save(entity);
            
            VoiceCall savedCall = voiceCallMapper.toDomain(entity);
            
            // 4. Publier événement
            publishCallEvent(TOPIC_CALL_INITIATED, savedCall);
            
            log.info("Appel vocal initié avec succès : callId={}", savedCall.getId());
            return savedCall;
            
        } catch (Exception e) {
            log.error("Erreur lors de l'initiation de l'appel", e);
            throw new AudioProcessingException("Échec de l'initiation de l'appel", e);
        }
    }
    
    /**
     * Met à jour le statut d'un appel vocal.
     * 
     * @param callId ID de l'appel
     * @param newStatus Nouveau statut
     * @return L'appel mis à jour
     */
    @Transactional
    public VoiceCall updateCallStatus(UUID callId, VoiceCall.CallStatus newStatus) {
        log.info("Mise à jour du statut de l'appel : callId={}, newStatus={}", 
                callId, newStatus);
        
        VoiceCallEntity entity = voiceCallRepository.findById(callId)
                .orElseThrow(() -> new AudioProcessingException(
                        "Appel introuvable : " + callId
                ));
        
        entity.setStatus(newStatus.name());
        
        // Si l'appel est connecté, enregistrer le timestamp
        if (newStatus == VoiceCall.CallStatus.CONNECTED && entity.getStartedAt() == null) {
            entity.setStartedAt(LocalDateTime.now());
            
            VoiceCall call = voiceCallMapper.toDomain(entity);
            publishCallEvent(TOPIC_CALL_CONNECTED, call);
        }
        
        entity = voiceCallRepository.save(entity);
        return voiceCallMapper.toDomain(entity);
    }
    
    /**
     * Termine un appel vocal et enregistre les métriques.
     * 
     * @param callId ID de l'appel
     * @param qualityMetrics Métriques de qualité collectées
     * @return L'appel terminé
     */
    @Transactional
    public VoiceCall endCall(UUID callId, java.util.Map<String, Object> qualityMetrics) {
        log.info("Terminaison de l'appel : callId={}", callId);
        
        VoiceCallEntity entity = voiceCallRepository.findById(callId)
                .orElseThrow(() -> new AudioProcessingException(
                        "Appel introuvable : " + callId
                ));
        
        LocalDateTime endTime = LocalDateTime.now();
        entity.setEndedAt(endTime);
        entity.setStatus(VoiceCall.CallStatus.ENDED.name());
        
        // Calculer la durée si l'appel était connecté
        if (entity.getStartedAt() != null) {
            Duration duration = Duration.between(entity.getStartedAt(), endTime);
            entity.setDurationSeconds((int) duration.getSeconds());
        }
        
        // Enregistrer les métriques de qualité
        entity.setQualityMetrics(qualityMetrics);
        
        entity = voiceCallRepository.save(entity);
        
        VoiceCall endedCall = voiceCallMapper.toDomain(entity);
        
        // Publier événement de fin d'appel
        publishCallEvent(TOPIC_CALL_ENDED, endedCall);
        
        log.info("Appel terminé : callId={}, durée={}s", 
                callId, entity.getDurationSeconds());
        
        return endedCall;
    }
    
    /**
     * Récupère un appel vocal par son ID.
     * 
     * @param callId ID de l'appel
     * @return L'appel trouvé
     */
    @Transactional(readOnly = true)
    public VoiceCall getCallById(UUID callId) {
        return voiceCallRepository.findById(callId)
                .map(voiceCallMapper::toDomain)
                .orElseThrow(() -> new AudioProcessingException(
                        "Appel introuvable : " + callId
                ));
    }
    
    /**
     * Récupère tous les appels d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Liste des appels
     */
    @Transactional(readOnly = true)
    public List<VoiceCall> getCallsByUser(UUID userId) {
        return voiceCallRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(voiceCallMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère l'appel actif d'un utilisateur (s'il existe).
     * 
     * @param userId ID de l'utilisateur
     * @return L'appel actif ou null
     */
    @Transactional(readOnly = true)
    public VoiceCall getActiveCall(UUID userId) {
        return voiceCallRepository
                .findByUserIdAndStatus(userId, VoiceCall.CallStatus.CONNECTED.name())
                .map(voiceCallMapper::toDomain)
                .orElse(null);
    }
    
    /**
     * Publie un événement d'appel vocal sur Kafka.
     * 
     * @param topic Topic Kafka
     * @param call Appel vocal
     */
    private void publishCallEvent(String topic, VoiceCall call) {
        try {
            kafkaTemplate.send(topic, call.getId().toString(), call);
            log.debug("Événement publié sur Kafka : topic={}", topic);
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement Kafka", e);
        }
    }
}

package com.nexusai.audio.core.service;

import com.nexusai.audio.core.domain.VoiceProfile;
import com.nexusai.audio.core.exception.AudioProcessingException;
import com.nexusai.audio.persistence.entity.VoiceProfileEntity;
import com.nexusai.audio.persistence.repository.VoiceProfileRepository;
import com.nexusai.audio.persistence.mapper.VoiceProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service de gestion des profils vocaux des compagnons IA.
 * 
 * <p>Ce service permet de configurer et personnaliser la voix de chaque
 * compagnon IA, utilisée lors de la synthèse vocale (TTS).</p>
 * 
 * <p>Chaque compagnon peut avoir :</p>
 * <ul>
 *   <li>Une voix prédéfinie du catalogue du fournisseur TTS</li>
 *   <li>Une voix clonée personnalisée</li>
 *   <li>Des paramètres de personnalisation (pitch, speed, style)</li>
 * </ul>
 * 
 * <p><strong>Exemple d'utilisation :</strong></p>
 * <pre>{@code
 * // Créer un profil vocal avec une voix ElevenLabs
 * VoiceProfile profile = VoiceProfile.builder()
 *     .companionId("companion-123")
 *     .provider("ELEVENLABS")
 *     .voiceId("21m00Tcm4TlvDq8ikWAM") // Rachel
 *     .pitch(0.0f)
 *     .speed(1.0f)
 *     .style("conversational")
 *     .build();
 * 
 * voiceProfileService.createVoiceProfile(profile);
 * }</pre>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceProfileService {
    
    private final VoiceProfileRepository voiceProfileRepository;
    private final VoiceProfileMapper voiceProfileMapper;
    
    /**
     * Crée un nouveau profil vocal pour un compagnon.
     * 
     * @param voiceProfile Profil vocal à créer
     * @return Le profil vocal créé
     * @throws AudioProcessingException Si un profil existe déjà pour ce compagnon
     */
    @Transactional
    public VoiceProfile createVoiceProfile(VoiceProfile voiceProfile) {
        log.info("Création d'un profil vocal pour companionId={}", 
                voiceProfile.getCompanionId());
        
        // Vérifier qu'il n'existe pas déjà un profil
        if (voiceProfileRepository.existsByCompanionId(voiceProfile.getCompanionId())) {
            throw new AudioProcessingException(
                    "Un profil vocal existe déjà pour ce compagnon : " + 
                    voiceProfile.getCompanionId()
            );
        }
        
        // Valider les paramètres
        validateVoiceProfile(voiceProfile);
        
        // Générer l'ID et les timestamps
        voiceProfile.setId(UUID.randomUUID());
        voiceProfile.setCreatedAt(LocalDateTime.now());
        voiceProfile.setUpdatedAt(LocalDateTime.now());
        
        // Sauvegarder
        VoiceProfileEntity entity = voiceProfileMapper.toEntity(voiceProfile);
        entity = voiceProfileRepository.save(entity);
        
        VoiceProfile saved = voiceProfileMapper.toDomain(entity);
        log.info("Profil vocal créé avec succès : id={}", saved.getId());
        
        return saved;
    }
    
    /**
     * Met à jour un profil vocal existant.
     * 
     * @param companionId ID du compagnon
     * @param updates Mises à jour à appliquer
     * @return Le profil mis à jour
     */
    @Transactional
    public VoiceProfile updateVoiceProfile(String companionId, VoiceProfile updates) {
        log.info("Mise à jour du profil vocal : companionId={}", companionId);
        
        VoiceProfileEntity entity = voiceProfileRepository
                .findByCompanionId(companionId)
                .orElseThrow(() -> new AudioProcessingException(
                        "Profil vocal introuvable pour le compagnon : " + companionId
                ));
        
        // Appliquer les mises à jour
        if (updates.getProvider() != null) {
            entity.setProvider(updates.getProvider());
        }
        if (updates.getVoiceId() != null) {
            entity.setVoiceId(updates.getVoiceId());
        }
        if (updates.getPitch() != null) {
            entity.setPitch(updates.getPitch());
        }
        if (updates.getSpeed() != null) {
            entity.setSpeed(updates.getSpeed());
        }
        if (updates.getStyle() != null) {
            entity.setStyle(updates.getStyle());
        }
        if (updates.getCustomVoiceUrl() != null) {
            entity.setCustomVoiceUrl(updates.getCustomVoiceUrl());
        }
        
        entity.setUpdatedAt(LocalDateTime.now());
        
        // Valider les nouvelles valeurs
        VoiceProfile updated = voiceProfileMapper.toDomain(entity);
        validateVoiceProfile(updated);
        
        // Sauvegarder
        entity = voiceProfileRepository.save(entity);
        
        log.info("Profil vocal mis à jour avec succès");
        return voiceProfileMapper.toDomain(entity);
    }
    
    /**
     * Récupère le profil vocal d'un compagnon.
     * 
     * @param companionId ID du compagnon
     * @return Le profil vocal
     */
    @Transactional(readOnly = true)
    public VoiceProfile getVoiceProfile(String companionId) {
        return voiceProfileRepository.findByCompanionId(companionId)
                .map(voiceProfileMapper::toDomain)
                .orElseThrow(() -> new AudioProcessingException(
                        "Profil vocal introuvable pour le compagnon : " + companionId
                ));
    }
    
    /**
     * Supprime le profil vocal d'un compagnon.
     * 
     * @param companionId ID du compagnon
     */
    @Transactional
    public void deleteVoiceProfile(String companionId) {
        log.info("Suppression du profil vocal : companionId={}", companionId);
        
        VoiceProfileEntity entity = voiceProfileRepository
                .findByCompanionId(companionId)
                .orElseThrow(() -> new AudioProcessingException(
                        "Profil vocal introuvable pour le compagnon : " + companionId
                ));
        
        voiceProfileRepository.delete(entity);
        log.info("Profil vocal supprimé avec succès");
    }
    
    /**
     * Vérifie si un compagnon possède un profil vocal.
     * 
     * @param companionId ID du compagnon
     * @return true si un profil existe
     */
    @Transactional(readOnly = true)
    public boolean hasVoiceProfile(String companionId) {
        return voiceProfileRepository.existsByCompanionId(companionId);
    }
    
    /**
     * Valide les paramètres d'un profil vocal.
     * 
     * @param profile Profil à valider
     * @throws AudioProcessingException Si la validation échoue
     */
    private void validateVoiceProfile(VoiceProfile profile) {
        // Validation du pitch (-1.0 à 1.0)
        if (profile.getPitch() != null && 
            (profile.getPitch() < -1.0f || profile.getPitch() > 1.0f)) {
            throw new AudioProcessingException(
                    "Le pitch doit être entre -1.0 et 1.0"
            );
        }
        
        // Validation de la vitesse (0.5 à 2.0)
        if (profile.getSpeed() != null && 
            (profile.getSpeed() < 0.5f || profile.getSpeed() > 2.0f)) {
            throw new AudioProcessingException(
                    "La vitesse doit être entre 0.5 et 2.0"
            );
        }
        
        // Au moins un de voiceId ou customVoiceUrl doit être défini
        if (profile.getVoiceId() == null && profile.getCustomVoiceUrl() == null) {
            throw new AudioProcessingException(
                    "voiceId ou customVoiceUrl doit être défini"
            );
        }
    }
}

// ===========================================
// SERVICE D'ANALYSE ÉMOTIONNELLE
// ===========================================

package com.nexusai.audio.core.service;

import com.nexusai.audio.emotion.service.EmotionAnalysisService;
import com.nexusai.audio.emotion.model.EmotionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service de détection d'émotions dans les messages vocaux.
 * 
 * <p>Ce service analyse le contenu vocal pour détecter l'état émotionnel
 * de l'utilisateur. Les émotions détectées peuvent être :</p>
 * <ul>
 *   <li>NEUTRAL - Neutre</li>
 *   <li>HAPPY - Joyeux</li>
 *   <li>SAD - Triste</li>
 *   <li>ANGRY - En colère</li>
 *   <li>ANXIOUS - Anxieux</li>
 *   <li>EXCITED - Excité</li>
 * </ul>
 * 
 * <p>L'analyse émotionnelle aide le compagnon IA à adapter ses réponses
 * en fonction de l'état émotionnel de l'utilisateur.</p>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmotionDetectionService {
    
    private final EmotionAnalysisService emotionAnalysisService;
    
    /**
     * Détecte l'émotion dans un message vocal.
     * 
     * @param audioData Données audio brutes
     * @return Résultat de l'analyse émotionnelle
     */
    public EmotionResult detectEmotion(byte[] audioData) {
        log.debug("Détection d'émotion sur un message vocal");
        
        try {
            EmotionResult result = emotionAnalysisService.analyze(audioData);
            
            log.info("Émotion détectée : {} (confiance: {})", 
                    result.getEmotion(), result.getConfidence());
            
            return result;
            
        } catch (Exception e) {
            log.error("Erreur lors de la détection d'émotion", e);
            // Retourner un résultat neutre par défaut en cas d'erreur
            return EmotionResult.builder()
                    .emotion("NEUTRAL")
                    .confidence(0.5f)
                    .build();
        }
    }
}
