package com.nexusai.video.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusai.video.domain.entity.GeneratedVideo;
import com.nexusai.video.repository.GeneratedVideoRepository;
import com.nexusai.video.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Listener Kafka pour les événements de progression de génération vidéo.
 * 
 * Écoute le topic "video.generation.events" et met à jour le statut
 * des vidéos en base de données en fonction des événements reçus.
 * 
 * Événements traités:
 * - PHASE_STARTED : Changement de phase
 * - PROGRESS_UPDATE : Mise à jour de la progression
 * - GENERATION_COMPLETED : Génération terminée avec succès
 * - GENERATION_FAILED : Génération échouée
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoEventListener {

    private final GeneratedVideoRepository videoRepository;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    /**
     * Écoute les événements de progression des générations vidéo.
     * 
     * @param message Message JSON contenant les détails de l'événement
     * @param acknowledgment Acknowledgment Kafka pour confirmer le traitement
     */
    @KafkaListener(
        topics = "${kafka.topics.video-generation-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleVideoEvent(
            @Payload String message,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("Réception d'un événement vidéo: {}", message);

            // Parsing du message
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = (String) event.get("eventType");
            String videoIdStr = (String) event.get("videoId");
            UUID videoId = UUID.fromString(videoIdStr);

            // Traitement selon le type d'événement
            switch (eventType) {
                case "PHASE_STARTED" -> handlePhaseStarted(videoId, event);
                case "PROGRESS_UPDATE" -> handleProgressUpdate(videoId, event);
                case "GENERATION_COMPLETED" -> handleGenerationCompleted(videoId, event);
                case "GENERATION_FAILED" -> handleGenerationFailed(videoId, event);
                default -> log.warn("Type d'événement inconnu: {}", eventType);
            }

            // Confirmer le traitement
            acknowledgment.acknowledge();
            log.debug("Événement traité avec succès pour la vidéo {}", videoId);

        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement vidéo", e);
            // Ne pas confirmer - le message sera retraité
        }
    }

    /**
     * Gère le démarrage d'une nouvelle phase.
     */
    private void handlePhaseStarted(UUID videoId, Map<String, Object> event) {
        String phase = (String) event.get("phase");
        
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus(GeneratedVideo.VideoStatus.PROCESSING);
            video.setCurrentPhase(GeneratedVideo.VideoPhase.valueOf(phase));
            videoRepository.save(video);
            
            log.info("Vidéo {} - Phase {} démarrée", videoId, phase);
        });
    }

    /**
     * Gère la mise à jour de progression.
     */
    private void handleProgressUpdate(UUID videoId, Map<String, Object> event) {
        Integer percentage = (Integer) event.get("percentage");
        String phase = (String) event.get("phase");
        
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setProgressPercentage(percentage);
            if (phase != null) {
                video.setCurrentPhase(GeneratedVideo.VideoPhase.valueOf(phase));
            }
            videoRepository.save(video);
            
            log.debug("Vidéo {} - Progression: {}%", videoId, percentage);
        });
    }

    /**
     * Gère la complétion d'une génération.
     */
    private void handleGenerationCompleted(UUID videoId, Map<String, Object> event) {
        String storageUrl = (String) event.get("storageUrl");
        String[] thumbnailUrls = ((java.util.List<String>) event.get("thumbnailUrls"))
            .toArray(new String[0]);
        Double fileSizeMb = ((Number) event.get("fileSizeMb")).doubleValue();
        Integer generationTimeMinutes = (Integer) event.get("generationTimeMinutes");
        
        videoRepository.findById(videoId).ifPresent(video -> {
            video.markAsCompleted(
                storageUrl,
                thumbnailUrls,
                BigDecimal.valueOf(fileSizeMb)
            );
            video.setGenerationTimeMinutes(generationTimeMinutes);
            videoRepository.save(video);
            
            // Consommer les jetons réservés
            tokenService.consumeTokens(video.getUserId(), videoId.toString());
            
            // Notifier l'utilisateur
            notificationService.sendVideoCompletedNotification(
                video.getUserId(),
                videoId,
                storageUrl
            );
            
            log.info("Vidéo {} terminée avec succès", videoId);
        });
    }

    /**
     * Gère l'échec d'une génération.
     */
    private void handleGenerationFailed(UUID videoId, Map<String, Object> event) {
        String errorMessage = (String) event.get("errorMessage");
        
        videoRepository.findById(videoId).ifPresent(video -> {
            video.markAsFailed(errorMessage);
            videoRepository.save(video);
            
            // Libérer les jetons réservés
            tokenService.releaseTokens(video.getUserId(), videoId.toString());
            
            // Notifier l'utilisateur
            notificationService.sendVideoFailedNotification(
                video.getUserId(),
                videoId,
                errorMessage
            );
            
            log.error("Échec de la génération de la vidéo {}: {}", videoId, errorMessage);
        });
    }
}

// ============================================================================
// SERVICE DE NOTIFICATION
// ============================================================================

package com.nexusai.video.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service d'envoi de notifications aux utilisateurs.
 * 
 * Envoie des notifications via Kafka vers le service de notification
 * qui les dispatche ensuite vers les différents canaux (WebSocket, Push, Email).
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final String TOPIC_NOTIFICATIONS = "user.notifications";

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    /**
     * Envoie une notification de complétion de vidéo.
     * 
     * @param userId ID de l'utilisateur
     * @param videoId ID de la vidéo
     * @param videoUrl URL de la vidéo
     */
    public void sendVideoCompletedNotification(
            UUID userId, 
            UUID videoId, 
            String videoUrl) {
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId.toString());
        notification.put("type", "VIDEO_COMPLETED");
        notification.put("title", "Votre vidéo est prête !");
        notification.put("message", "La génération de votre vidéo est terminée.");
        notification.put("data", Map.of(
            "videoId", videoId.toString(),
            "videoUrl", videoUrl
        ));
        notification.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(TOPIC_NOTIFICATIONS, userId.toString(), notification);
        log.info("Notification de complétion envoyée pour la vidéo {}", videoId);
    }

    /**
     * Envoie une notification d'échec de génération.
     * 
     * @param userId ID de l'utilisateur
     * @param videoId ID de la vidéo
     * @param errorMessage Message d'erreur
     */
    public void sendVideoFailedNotification(
            UUID userId, 
            UUID videoId, 
            String errorMessage) {
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId.toString());
        notification.put("type", "VIDEO_FAILED");
        notification.put("title", "Échec de génération");
        notification.put("message", "La génération de votre vidéo a échoué.");
        notification.put("data", Map.of(
            "videoId", videoId.toString(),
            "error", errorMessage
        ));
        notification.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(TOPIC_NOTIFICATIONS, userId.toString(), notification);
        log.info("Notification d'échec envoyée pour la vidéo {}", videoId);
    }

    /**
     * Envoie une notification de progression.
     * 
     * @param userId ID de l'utilisateur
     * @param videoId ID de la vidéo
     * @param phase Phase actuelle
     * @param percentage Pourcentage
     */
    public void sendProgressNotification(
            UUID userId,
            UUID videoId,
            String phase,
            int percentage) {
        
        // Envoyer uniquement à certains jalons (25%, 50%, 75%)
        if (percentage % 25 != 0) {
            return;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId.toString());
        notification.put("type", "VIDEO_PROGRESS");
        notification.put("title", "Génération en cours");
        notification.put("message", String.format("Votre vidéo est à %d%% (%s)", percentage, phase));
        notification.put("data", Map.of(
            "videoId", videoId.toString(),
            "phase", phase,
            "percentage", percentage
        ));
        notification.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(TOPIC_NOTIFICATIONS, userId.toString(), notification);
    }
}

// ============================================================================
// CONFIGURATION KAFKA
// ============================================================================

package com.nexusai.video.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Kafka pour le module vidéo.
 * 
 * Configure les producers et consumers Kafka avec les paramètres
 * appropriés pour la génération vidéo.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Configuration
@EnableKafka
public class KafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Configuration du producer Kafka pour les requêtes de génération.
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Configuration du producer pour les notifications (avec sérialisation JSON).
     */
    @Bean
    public ProducerFactory<String, Map<String, Object>> notificationProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Map<String, Object>> notificationKafkaTemplate() {
        return new KafkaTemplate<>(notificationProducerFactory());
    }

    /**
     * Configuration du consumer Kafka pour les événements vidéo.
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Configuration du listener container avec acknowledgment manuel.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> 
            kafkaListenerContainerFactory() {
        
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // 3 consumers en parallèle
        factory.getContainerProperties()
            .setAckMode(ContainerProperties.AckMode.MANUAL);
        
        return factory;
    }
}

// ============================================================================
// CONFIGURATION APPLICATION
// ============================================================================

package com.nexusai.video;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe principale de l'application Video Generation.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@SpringBootApplication
@EnableKafka
@EnableAsync
@EnableScheduling
public class VideoGenerationApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoGenerationApplication.class, args);
    }
}
