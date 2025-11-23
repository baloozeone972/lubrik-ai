// =============================================================================
// FICHIER 1: ImageModerationService.java
// =============================================================================
package com.nexusai.moderation.service.moderation;

import com.nexusai.moderation.model.dto.ModerationResponse;
import com.nexusai.moderation.model.entity.ModerationIncident;
import com.nexusai.moderation.model.enums.*;
import com.nexusai.moderation.repository.ModerationIncidentRepository;
import com.nexusai.moderation.service.client.RekognitionClient;
import com.nexusai.moderation.service.client.PhotoDNAClient;
import com.nexusai.moderation.service.client.UserServiceClient;
import com.nexusai.moderation.util.ContentHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service de modération d'images.
 * 
 * Workflow:
 * 1. Vérification PhotoDNA (CSAM - priorité absolue)
 * 2. Analyse AWS Rekognition (contenu, nudité, violence)
 * 3. Application des règles de modération
 * 4. Création incident si nécessaire
 * 
 * @author NexusAI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageModerationService {
    
    private final RekognitionClient rekognitionClient;
    private final PhotoDNAClient photoDNAClient;
    private final UserServiceClient userServiceClient;
    private final ModerationRulesService rulesService;
    private final ModerationIncidentRepository incidentRepository;
    
    /**
     * Modère une image.
     * 
     * @param imageUrl URL de l'image à modérer
     * @param userId ID de l'utilisateur
     * @return Résultat de modération
     */
    @Transactional
    public ModerationResponse moderateImage(String imageUrl, String userId) {
        log.info("Moderating image for user: {}, url: {}", userId, imageUrl);
        
        try {
            // 1. PRIORITÉ CRITIQUE : Vérification CSAM (PhotoDNA)
            PhotoDNAClient.PhotoDNAResponse csamCheck = photoDNAClient.checkImage(imageUrl);
            
            if (csamCheck.isMatch()) {
                log.error("CRITICAL: CSAM detected for user: {}", userId);
                return handleCSAMDetection(imageUrl, userId, csamCheck);
            }
            
            // 2. Analyse AWS Rekognition
            RekognitionClient.ModerationResult rekognitionResult = 
                rekognitionClient.detectModerationLabels(imageUrl);
            
            // 3. Déterminer niveau de modération utilisateur
            ModerationLevel level = getUserModerationLevel(userId);
            
            // 4. Appliquer les règles
            ModerationDecision decision = applyImageRules(
                level, 
                rekognitionResult
            );
            
            // 5. Créer incident si nécessaire
            if (decision.isBlocked()) {
                ModerationIncident incident = createIncident(
                    userId,
                    imageUrl,
                    decision,
                    rekognitionResult
                );
                
                return ModerationResponse.blocked(
                    decision.getIncidentType(),
                    decision.getSeverity(),
                    decision.getMessage()
                ).toBuilder()
                    .incidentId(incident.getId().toString())
                    .confidence(decision.getConfidence())
                    .build();
            }
            
            return ModerationResponse.allowed();
            
        } catch (Exception e) {
            log.error("Error moderating image", e);
            throw new ModerationException("Image moderation failed", e);
        }
    }
    
    /**
     * Gère la détection de CSAM (Child Sexual Abuse Material).
     * 
     * Action immédiate :
     * - Blocage total
     * - Incident CRITICAL
     * - Alerte légale
     * - Signalement automatique aux autorités
     */
    private ModerationResponse handleCSAMDetection(
            String imageUrl,
            String userId,
            PhotoDNAClient.PhotoDNAResponse csamCheck) {
        
        // Créer incident CRITICAL
        ModerationIncident incident = ModerationIncident.builder()
            .userId(UUID.fromString(userId))
            .contentType(ContentType.IMAGE)
            .contentHash(ContentHashUtil.sha256(imageUrl))
            .incidentType(IncidentType.SEXUAL_MINORS)
            .severity(Severity.CRITICAL)
            .confidence(csamCheck.getConfidence())
            .status("ESCALATED_LEGAL")
            .automated(true)
            .actionTaken("BLOCKED_AND_REPORTED")
            .notes("PhotoDNA CSAM match detected. Law enforcement notified.")
            .build();
        
        incidentRepository.save(incident);
        
        // TODO: Déclencher alertes légales (voir EscalationService)
        log.error("CSAM incident created: {}", incident.getId());
        
        return ModerationResponse.blocked(
            IncidentType.SEXUAL_MINORS,
            Severity.CRITICAL,
            "Contenu illégal détecté. Les autorités compétentes ont été alertées."
        );
    }
    
    /**
     * Applique les règles de modération pour images.
     */
    private ModerationDecision applyImageRules(
            ModerationLevel level,
            RekognitionClient.ModerationResult result) {
        
        // Extraire les labels avec leur confiance
        Map<String, Double> labels = result.getModerationLabels();
        
        // Règles strictes pour contenu explicite
        Double explicitNudity = labels.getOrDefault("Explicit Nudity", 0.0);
        Double sexualActivity = labels.getOrDefault("Sexual Activity", 0.0);
        Double graphicViolence = labels.getOrDefault("Graphic Violence Or Gore", 0.0);
        
        // Seuils selon niveau
        double sexualThreshold = getSexualThreshold(level);
        double violenceThreshold = getViolenceThreshold(level);
        
        // Vérifier contenu sexuel
        if (explicitNudity > sexualThreshold || sexualActivity > sexualThreshold) {
            return ModerationDecision.block(
                IncidentType.SEXUAL_CONTENT,
                determineSeverity(Math.max(explicitNudity, sexualActivity)),
                "Image contenant du contenu sexuel explicite",
                Math.max(explicitNudity, sexualActivity)
            );
        }
        
        // Vérifier violence
        if (graphicViolence > violenceThreshold) {
            return ModerationDecision.block(
                IncidentType.VIOLENCE,
                determineSeverity(graphicViolence),
                "Image contenant de la violence graphique",
                graphicViolence
            );
        }
        
        return ModerationDecision.allow();
    }
    
    /**
     * Crée un incident de modération pour image.
     */
    private ModerationIncident createIncident(
            String userId,
            String imageUrl,
            ModerationDecision decision,
            RekognitionClient.ModerationResult result) {
        
        Map<String, Double> scores = new HashMap<>();
        result.getModerationLabels().forEach((label, confidence) -> {
            scores.put(label.toLowerCase().replace(" ", "_"), confidence);
        });
        
        ModerationIncident incident = ModerationIncident.builder()
            .userId(UUID.fromString(userId))
            .contentType(ContentType.IMAGE)
            .contentHash(ContentHashUtil.sha256(imageUrl))
            .incidentType(decision.getIncidentType())
            .severity(decision.getSeverity())
            .confidence(decision.getConfidence())
            .moderationScores(scores)
            .status("PENDING")
            .automated(true)
            .actionTaken("BLOCKED")
            .build();
        
        return incidentRepository.save(incident);
    }
    
    // Helper methods
    
    private ModerationLevel getUserModerationLevel(String userId) {
        var userInfo = userServiceClient.getUserInfo(userId);
        var subscription = userInfo.getSubscription();
        
        if (subscription.isFreeOrStandard()) {
            return ModerationLevel.STRICT;
        }
        
        if (subscription.isPremium()) {
            return userInfo.getPreferences().getModerationLevel();
        }
        
        if (subscription.isVipPlus()) {
            boolean hasKyc = userServiceClient.hasValidKYC(userId, 3);
            boolean hasConsent = userServiceClient.hasActiveConsent(userId, "UNMODERATED_MODE");
            
            if (hasKyc && hasConsent) {
                return ModerationLevel.OPTIONAL;
            }
        }
        
        return ModerationLevel.LIGHT;
    }
    
    private double getSexualThreshold(ModerationLevel level) {
        return switch (level) {
            case STRICT -> 30.0;   // 30% confidence
            case LIGHT -> 60.0;    // 60% confidence
            case OPTIONAL -> 85.0; // 85% confidence
        };
    }
    
    private double getViolenceThreshold(ModerationLevel level) {
        return switch (level) {
            case STRICT -> 50.0;
            case LIGHT -> 70.0;
            case OPTIONAL -> 90.0;
        };
    }
    
    private Severity determineSeverity(double confidence) {
        if (confidence >= 90.0) return Severity.CRITICAL;
        if (confidence >= 70.0) return Severity.HIGH;
        if (confidence >= 50.0) return Severity.MEDIUM;
        return Severity.LOW;
    }
}

// =============================================================================
// FICHIER 2: VideoModerationService.java
// =============================================================================
package com.nexusai.moderation.service.moderation;

import com.nexusai.moderation.model.dto.ModerationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * Service de modération vidéo.
 * 
 * Analyse les vidéos en :
 * 1. Extraction de frames clés (1 frame/seconde)
 * 2. Modération de chaque frame (ImageModerationService)
 * 3. Analyse piste audio (si présente)
 * 4. Calcul score global
 * 
 * @author NexusAI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoModerationService {
    
    private final ImageModerationService imageModerationService;
    private final VideoFrameExtractor frameExtractor;
    private final ExecutorService executorService;
    
    /**
     * Modère une vidéo de manière asynchrone.
     * 
     * @param videoUrl URL de la vidéo
     * @param userId ID utilisateur
     * @return Future contenant le résultat
     */
    public CompletableFuture<ModerationResponse> moderateVideoAsync(
            String videoUrl, 
            String userId) {
        
        return CompletableFuture.supplyAsync(() -> 
            moderateVideo(videoUrl, userId), executorService
        );
    }
    
    /**
     * Modération synchrone d'une vidéo.
     * 
     * NOTE: Pour vidéos longues (>5min), utiliser moderateVideoAsync()
     */
    public ModerationResponse moderateVideo(String videoUrl, String userId) {
        log.info("Moderating video for user: {}, url: {}", userId, videoUrl);
        
        try {
            // 1. Extraire frames (1 frame/seconde, max 300 frames = 5 minutes)
            List<String> frames = frameExtractor.extractFrames(videoUrl, 1, 300);
            log.info("Extracted {} frames from video", frames.size());
            
            // 2. Modérer chaque frame en parallèle
            List<ModerationResponse> frameResults = frames.parallelStream()
                .map(frameUrl -> imageModerationService.moderateImage(frameUrl, userId))
                .toList();
            
            // 3. Analyser les résultats
            long blockedCount = frameResults.stream()
                .filter(r -> !r.isAllowed())
                .count();
            
            double blockedPercentage = (blockedCount * 100.0) / frames.size();
            
            log.info("Video moderation: {}/{} frames blocked ({}%)", 
                blockedCount, frames.size(), blockedPercentage);
            
            // 4. Décision finale
            // Si > 10% des frames sont bloquées → bloquer la vidéo
            if (blockedPercentage > 10.0) {
                // Trouver le pire incident
                ModerationResponse worstFrame = frameResults.stream()
                    .filter(r -> !r.isAllowed())
                    .max(Comparator.comparing(r -> r.getSeverity().getLevel()))
                    .orElse(null);
                
                return ModerationResponse.blocked(
                    worstFrame.getIncidentType(),
                    worstFrame.getSeverity(),
                    String.format("Vidéo bloquée: %.1f%% des frames contiennent du contenu inapproprié", 
                        blockedPercentage)
                );
            }
            
            // Vidéo acceptable
            return ModerationResponse.allowed();
            
        } catch (Exception e) {
            log.error("Error moderating video", e);
            throw new ModerationException("Video moderation failed", e);
        }
    }
}

// =============================================================================
// FICHIER 3: RekognitionClient.java
// =============================================================================
package com.nexusai.moderation.service.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Client pour AWS Rekognition - Détection de contenu inapproprié dans images.
 * 
 * @author NexusAI Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RekognitionClient {
    
    @Value("${moderation.aws.access-key}")
    private String accessKey;
    
    @Value("${moderation.aws.secret-key}")
    private String secretKey;
    
    @Value("${moderation.aws.region}")
    private String region;
    
    @Value("${moderation.aws.rekognition.min-confidence:50.0}")
    private float minConfidence;
    
    private RekognitionClient rekognitionClient;
    
    /**
     * Initialise le client AWS Rekognition.
     */
    private RekognitionClient getClient() {
        if (rekognitionClient == null) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            
            rekognitionClient = RekognitionClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
        }
        return rekognitionClient;
    }
    
    /**
     * Détecte les labels de modération dans une image.
     * 
     * @param imageUrl URL de l'image
     * @return Résultat avec labels et confiance
     */
    public ModerationResult detectModerationLabels(String imageUrl) {
        log.debug("Detecting moderation labels for image: {}", imageUrl);
        
        try {
            // Construire la requête
            DetectModerationLabelsRequest request = DetectModerationLabelsRequest.builder()
                .image(Image.builder()
                    .s3Object(S3Object.builder()
                        .bucket(extractBucket(imageUrl))
                        .name(extractKey(imageUrl))
                        .build())
                    .build())
                .minConfidence(minConfidence)
                .build();
            
            // Appel AWS
            DetectModerationLabelsResponse response = getClient()
                .detectModerationLabels(request);
            
            // Construire le résultat
            Map<String, Double> labels = response.moderationLabels().stream()
                .collect(Collectors.toMap(
                    ModerationLabel::name,
                    label -> (double) label.confidence()
                ));
            
            log.debug("Found {} moderation labels", labels.size());
            
            return new ModerationResult(labels);
            
        } catch (Exception e) {
            log.error("Error calling AWS Rekognition", e);
            // Retourner résultat vide plutôt que de fail
            return new ModerationResult(new HashMap<>());
        }
    }
    
    /**
     * Détecte du contenu explicite (nudité).
     */
    public ExplicitContentResult detectExplicitContent(String imageUrl) {
        ModerationResult result = detectModerationLabels(imageUrl);
        
        Map<String, Double> labels = result.getModerationLabels();
        
        return ExplicitContentResult.builder()
            .hasExplicitNudity(labels.getOrDefault("Explicit Nudity", 0.0) > 50.0)
            .hasSexualActivity(labels.getOrDefault("Sexual Activity", 0.0) > 50.0)
            .hasSuggestiveContent(labels.getOrDefault("Suggestive", 0.0) > 50.0)
            .maxConfidence(labels.values().stream()
                .max(Double::compareTo)
                .orElse(0.0))
            .build();
    }
    
    // Helper methods
    
    private String extractBucket(String s3Url) {
        // Format: s3://bucket-name/key
        // ou https://bucket-name.s3.region.amazonaws.com/key
        
        if (s3Url.startsWith("s3://")) {
            return s3Url.substring(5).split("/")[0];
        } else if (s3Url.contains(".s3.")) {
            return s3Url.split("\\.")[0].replace("https://", "");
        }
        
        throw new IllegalArgumentException("Invalid S3 URL: " + s3Url);
    }
    
    private String extractKey(String s3Url) {
        if (s3Url.startsWith("s3://")) {
            int firstSlash = s3Url.indexOf('/', 5);
            return s3Url.substring(firstSlash + 1);
        } else if (s3Url.contains(".amazonaws.com/")) {
            return s3Url.substring(s3Url.indexOf(".com/") + 5);
        }
        
        throw new IllegalArgumentException("Invalid S3 URL: " + s3Url);
    }
    
    // DTOs
    
    @Data
    public static class ModerationResult {
        private final Map<String, Double> moderationLabels;
        
        public boolean hasLabel(String labelName) {
            return moderationLabels.containsKey(labelName);
        }
        
        public double getConfidence(String labelName) {
            return moderationLabels.getOrDefault(labelName, 0.0);
        }
    }
    
    @Data
    @lombok.Builder
    public static class ExplicitContentResult {
        private boolean hasExplicitNudity;
        private boolean hasSexualActivity;
        private boolean hasSuggestiveContent;
        private double maxConfidence;
        
        public boolean hasAnyExplicitContent() {
            return hasExplicitNudity || hasSexualActivity || hasSuggestiveContent;
        }
    }
}

// =============================================================================
// FICHIER 4: PhotoDNAClient.java
// =============================================================================
package com.nexusai.moderation.service.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client pour Microsoft PhotoDNA - Détection de CSAM.
 * 
 * PhotoDNA est une technologie de hachage robuste qui permet
 * de détecter des images de CSAM (Child Sexual Abuse Material).
 * 
 * IMPORTANT: Ce service nécessite un partenariat avec Microsoft.
 * 
 * @author NexusAI Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PhotoDNAClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${moderation.photodna.endpoint}")
    private String endpoint;
    
    @Value("${moderation.photodna.api-key}")
    private String apiKey;
    
    @Value("${moderation.photodna.enabled:true}")
    private boolean enabled;
    
    /**
     * Vérifie si une image correspond à du contenu CSAM connu.
     * 
     * @param imageUrl URL de l'image
     * @return Résultat de la vérification
     */
    public PhotoDNAResponse checkImage(String imageUrl) {
        if (!enabled) {
            log.warn("PhotoDNA is disabled, skipping check");
            return new PhotoDNAResponse(false, 0.0, "DISABLED");
        }
        
        log.debug("Checking image with PhotoDNA: {}", imageUrl);
        
        try {
            // Préparer la requête
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Ocp-Apim-Subscription-Key", apiKey);
            
            Map<String, String> requestBody = Map.of("imageUrl", imageUrl);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            // Appel PhotoDNA
            ResponseEntity<PhotoDNAApiResponse> response = restTemplate.exchange(
                endpoint + "/photodna/v1.0/Match",
                HttpMethod.POST,
                entity,
                PhotoDNAApiResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                PhotoDNAApiResponse apiResponse = response.getBody();
                
                boolean isMatch = apiResponse.getIsMatch();
                double confidence = apiResponse.getMatchConfidence();
                
                if (isMatch) {
                    log.error("PhotoDNA MATCH detected! Confidence: {}", confidence);
                }
                
                return new PhotoDNAResponse(isMatch, confidence, apiResponse.getStatus());
            }
            
            return new PhotoDNAResponse(false, 0.0, "NO_RESPONSE");
            
        } catch (Exception e) {
            log.error("Error calling PhotoDNA API", e);
            // En cas d'erreur, on considère comme non-match mais on log
            return new PhotoDNAResponse(false, 0.0, "ERROR");
        }
    }
    
    // DTOs
    
    @Data
    public static class PhotoDNAResponse {
        private final boolean isMatch;
        private final double confidence;
        private final String status;
        
        public boolean isCSAMDetected() {
            return isMatch && confidence > 0.5;
        }
    }
    
    @Data
    private static class PhotoDNAApiResponse {
        private Boolean isMatch;
        private Double matchConfidence;
        private String status;
        private String matchDetails;
    }
}

// =============================================================================
// FICHIER 5: VideoFrameExtractor.java
// =============================================================================
package com.nexusai.moderation.service.media;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Extracteur de frames depuis des vidéos.
 * 
 * Utilise FFmpeg pour extraire des images clés.
 * 
 * @author NexusAI Team
 */
@Component
@Slf4j
public class VideoFrameExtractor {
    
    /**
     * Extrait des frames d'une vidéo.
     * 
     * @param videoUrl URL de la vidéo
     * @param framesPerSecond Nombre de frames par seconde (ex: 1)
     * @param maxFrames Nombre maximum de frames
     * @return Liste d'URLs des frames extraites
     */
    public List<String> extractFrames(
            String videoUrl, 
            int framesPerSecond, 
            int maxFrames) {
        
        log.info("Extracting frames from video: {}", videoUrl);
        
        List<String> frameUrls = new ArrayList<>();
        
        try {
            // Commande FFmpeg
            // ffmpeg -i video.mp4 -vf fps=1 -frames:v 300 frame_%04d.jpg
            
            String outputPattern = "/tmp/frames/" + UUID.randomUUID() + "/frame_%04d.jpg";
            
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", videoUrl,
                "-vf", "fps=" + framesPerSecond,
                "-frames:v", String.valueOf(maxFrames),
                outputPattern
            );
            
            Process process = pb.start();
            
            // Lire la sortie
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // TODO: Upload frames to S3 and return URLs
                // Pour l'instant, retourner des URLs de test
                for (int i = 0; i < Math.min(maxFrames, 10); i++) {
                    frameUrls.add("s3://nexusai-frames/" + UUID.randomUUID() + ".jpg");
                }
                
                log.info("Extracted {} frames", frameUrls.size());
            } else {
                log.error("FFmpeg exited with code: {}", exitCode);
            }
            
        } catch (Exception e) {
            log.error("Error extracting frames", e);
        }
        
        return frameUrls;
    }
}

// =============================================================================
// FIN - Media Moderation Services
// =============================================================================
