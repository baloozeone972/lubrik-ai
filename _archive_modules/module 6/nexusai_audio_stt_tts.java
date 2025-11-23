// ===========================================
// MODULE 6.3 : SPEECH-TO-TEXT (STT)
// ===========================================

package com.nexusai.audio.stt.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Résultat d'une transcription audio.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionResult {
    
    /**
     * Texte transcrit.
     */
    private String text;
    
    /**
     * Langue détectée (code ISO 639-1).
     */
    private String language;
    
    /**
     * Score de confiance (0.0 à 1.0).
     */
    private Float confidence;
    
    /**
     * Durée du traitement en millisecondes.
     */
    private Long processingTimeMs;
}

package com.nexusai.audio.stt.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client HTTP pour l'API OpenAI Whisper.
 * 
 * <p>Ce client communique avec l'API Whisper d'OpenAI pour effectuer
 * la transcription audio vers texte.</p>
 * 
 * <p><strong>Configuration requise dans application.yml :</strong></p>
 * <pre>{@code
 * openai:
 *   api-key: sk-...
 *   whisper:
 *     url: https://api.openai.com/v1/audio/transcriptions
 *     model: whisper-1
 * }</pre>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAIWhisperClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${openai.api-key}")
    private String apiKey;
    
    @Value("${openai.whisper.url:https://api.openai.com/v1/audio/transcriptions}")
    private String whisperUrl;
    
    @Value("${openai.whisper.model:whisper-1}")
    private String model;
    
    /**
     * Transcrit un fichier audio en texte.
     * 
     * @param audioData Données audio (MP3, WAV, M4A)
     * @param fileName Nom du fichier
     * @return Résultat de la transcription
     */
    public Map<String, Object> transcribe(byte[] audioData, String fileName) {
        log.debug("Appel API Whisper : file={}, size={} bytes", fileName, audioData.length);
        
        try {
            // Préparation de la requête multipart
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(apiKey);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(audioData) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            });
            body.add("model", model);
            body.add("response_format", "verbose_json"); // Inclut langue et timestamps
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = 
                new HttpEntity<>(body, headers);
            
            // Appel API
            long startTime = System.currentTimeMillis();
            
            ResponseEntity<Map> response = restTemplate.exchange(
                whisperUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            log.info("Transcription réussie : durée={}ms", processingTime);
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Erreur lors de la transcription Whisper", e);
            throw new RuntimeException("Échec de la transcription audio", e);
        }
    }
}

package com.nexusai.audio.stt.service;

import com.nexusai.audio.stt.client.OpenAIWhisperClient;
import com.nexusai.audio.stt.model.TranscriptionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Service de transcription Speech-to-Text utilisant OpenAI Whisper.
 * 
 * <p>Ce service transforme des fichiers audio en texte en utilisant
 * le modèle Whisper d'OpenAI, qui supporte :</p>
 * <ul>
 *   <li>Plus de 50 langues</li>
 *   <li>Formats : MP3, WAV, M4A, WebM, FLAC</li>
 *   <li>Fichiers jusqu'à 25 MB</li>
 *   <li>Détection automatique de la langue</li>
 * </ul>
 * 
 * <p><strong>Exemple d'utilisation :</strong></p>
 * <pre>{@code
 * MultipartFile audioFile = ...; // Fichier MP3
 * TranscriptionResult result = whisperService.transcribe(audioFile);
 * 
 * System.out.println("Texte: " + result.getText());
 * System.out.println("Langue: " + result.getLanguage());
 * }</pre>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhisperSTTService {
    
    private final OpenAIWhisperClient whisperClient;
    
    /**
     * Transcrit un fichier audio en texte.
     * 
     * @param audioFile Fichier audio à transcrire
     * @return Résultat de la transcription
     * @throws RuntimeException Si la transcription échoue
     */
    public TranscriptionResult transcribe(MultipartFile audioFile) {
        log.info("Transcription d'un fichier audio : name={}, size={}", 
                audioFile.getOriginalFilename(), audioFile.getSize());
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Appel API Whisper
            byte[] audioData = audioFile.getBytes();
            Map<String, Object> response = whisperClient.transcribe(
                audioData, 
                audioFile.getOriginalFilename()
            );
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Construction du résultat
            TranscriptionResult result = TranscriptionResult.builder()
                    .text((String) response.get("text"))
                    .language((String) response.get("language"))
                    .confidence(1.0f) // Whisper ne retourne pas de score de confiance
                    .processingTimeMs(processingTime)
                    .build();
            
            log.info("Transcription réussie : langue={}, longueur={} caractères", 
                    result.getLanguage(), result.getText().length());
            
            return result;
            
        } catch (Exception e) {
            log.error("Erreur lors de la transcription", e);
            throw new RuntimeException("Échec de la transcription audio", e);
        }
    }
    
    /**
     * Transcrit des données audio brutes.
     * 
     * @param audioData Données audio
     * @param fileName Nom du fichier (avec extension)
     * @return Résultat de la transcription
     */
    public TranscriptionResult transcribe(byte[] audioData, String fileName) {
        log.info("Transcription de données audio : size={} bytes", audioData.length);
        
        try {
            long startTime = System.currentTimeMillis();
            
            Map<String, Object> response = whisperClient.transcribe(audioData, fileName);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            return TranscriptionResult.builder()
                    .text((String) response.get("text"))
                    .language((String) response.get("language"))
                    .confidence(1.0f)
                    .processingTimeMs(processingTime)
                    .build();
                    
        } catch (Exception e) {
            log.error("Erreur lors de la transcription", e);
            throw new RuntimeException("Échec de la transcription audio", e);
        }
    }
}

// ===========================================
// MODULE 6.4 : TEXT-TO-SPEECH (TTS)
// ===========================================

package com.nexusai.audio.tts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Paramètres de configuration pour la voix.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSettings {
    
    /**
     * Stabilité de la voix (0.0 à 1.0).
     * Plus stable = moins de variation émotionnelle.
     */
    @Builder.Default
    private Float stability = 0.75f;
    
    /**
     * Boost de similarité (0.0 à 1.0).
     * Plus élevé = plus fidèle à la voix d'origine.
     */
    @Builder.Default
    private Float similarityBoost = 0.75f;
    
    /**
     * Exagération du style (0.0 à 1.0).
     */
    @Builder.Default
    private Float style = 0.0f;
    
    /**
     * Utilisation du boost de locuteur (boolean).
     */
    @Builder.Default
    private Boolean useSpeakerBoost = true;
}

package com.nexusai.audio.tts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Résultat d'une synthèse vocale.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SynthesisResult {
    
    /**
     * Données audio générées.
     */
    private byte[] audioData;
    
    /**
     * URL de stockage (si uploadé).
     */
    private String audioUrl;
    
    /**
     * Durée estimée en secondes.
     */
    private Integer durationSeconds;
    
    /**
     * Taille des données en octets.
     */
    private Long fileSizeBytes;
    
    /**
     * Temps de traitement en millisecondes.
     */
    private Long processingTimeMs;
}

package com.nexusai.audio.tts.client;

import com.nexusai.audio.tts.model.VoiceSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Client HTTP pour l'API ElevenLabs TTS.
 * 
 * <p>ElevenLabs fournit des voix IA ultra-réalistes pour la synthèse vocale.
 * Plus d'infos : https://elevenlabs.io/docs</p>
 * 
 * <p><strong>Configuration requise :</strong></p>
 * <pre>{@code
 * elevenlabs:
 *   api-key: your-api-key
 *   url: https://api.elevenlabs.io/v1
 * }</pre>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElevenLabsClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${elevenlabs.api-key}")
    private String apiKey;
    
    @Value("${elevenlabs.url:https://api.elevenlabs.io/v1}")
    private String baseUrl;
    
    /**
     * Synthétise du texte en audio.
     * 
     * @param text Texte à synthétiser
     * @param voiceId ID de la voix ElevenLabs
     * @param settings Paramètres vocaux
     * @return Données audio (MP3)
     */
    public byte[] textToSpeech(String text, String voiceId, VoiceSettings settings) {
        log.debug("Synthèse vocale ElevenLabs : voiceId={}, textLength={}", 
                voiceId, text.length());
        
        String url = String.format("%s/text-to-speech/%s", baseUrl, voiceId);
        
        try {
            // Préparation de la requête
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("xi-api-key", apiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", text);
            requestBody.put("model_id", "eleven_monolingual_v1");
            
            Map<String, Object> voiceSettingsMap = new HashMap<>();
            voiceSettingsMap.put("stability", settings.getStability());
            voiceSettingsMap.put("similarity_boost", settings.getSimilarityBoost());
            voiceSettingsMap.put("style", settings.getStyle());
            voiceSettingsMap.put("use_speaker_boost", settings.getUseSpeakerBoost());
            requestBody.put("voice_settings", voiceSettingsMap);
            
            HttpEntity<Map<String, Object>> requestEntity = 
                new HttpEntity<>(requestBody, headers);
            
            // Appel API
            long startTime = System.currentTimeMillis();
            
            ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                byte[].class
            );
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            byte[] audioData = response.getBody();
            
            log.info("Synthèse vocale réussie : durée={}ms, size={} bytes", 
                    processingTime, audioData.length);
            
            return audioData;
            
        } catch (Exception e) {
            log.error("Erreur lors de la synthèse vocale ElevenLabs", e);
            throw new RuntimeException("Échec de la synthèse vocale", e);
        }
    }
}

package com.nexusai.audio.tts.service;

import com.nexusai.audio.tts.client.ElevenLabsClient;
import com.nexusai.audio.tts.model.SynthesisResult;
import com.nexusai.audio.tts.model.VoiceSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service de synthèse vocale Text-to-Speech utilisant ElevenLabs.
 * 
 * <p>Ce service transforme du texte en parole avec des voix IA réalistes.
 * Il supporte :</p>
 * <ul>
 *   <li>Voix prédéfinies de haute qualité</li>
 *   <li>Clonage de voix personnalisées</li>
 *   <li>Contrôle fin de la stabilité et du style</li>
 *   <li>Multi-langues</li>
 * </ul>
 * 
 * <p><strong>Voix populaires ElevenLabs :</strong></p>
 * <ul>
 *   <li>21m00Tcm4TlvDq8ikWAM - Rachel (femme, américain)</li>
 *   <li>EXAVITQu4vr4xnSDxMaL - Sarah (femme, calme)</li>
 *   <li>VR6AewLTigWG4xSOukaG - Arnold (homme, grave)</li>
 * </ul>
 * 
 * <p><strong>Exemple d'utilisation :</strong></p>
 * <pre>{@code
 * String text = "Bonjour ! Comment puis-je t'aider aujourd'hui ?";
 * String voiceId = "21m00Tcm4TlvDq8ikWAM";
 * 
 * VoiceSettings settings = VoiceSettings.builder()
 *     .stability(0.75f)
 *     .similarityBoost(0.80f)
 *     .build();
 * 
 * SynthesisResult result = elevenLabsService.synthesize(text, voiceId, settings);
 * // result.getAudioData() contient le MP3
 * }</pre>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElevenLabsTTSService {
    
    private final ElevenLabsClient elevenLabsClient;
    
    /**
     * Synthétise du texte en audio.
     * 
     * @param text Texte à synthétiser
     * @param voiceId ID de la voix ElevenLabs
     * @param settings Paramètres vocaux
     * @return Résultat de la synthèse
     */
    public SynthesisResult synthesize(
            String text, 
            String voiceId, 
            VoiceSettings settings) {
        
        log.info("Synthèse vocale : voiceId={}, textLength={}", 
                voiceId, text.length());
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Appel ElevenLabs
            byte[] audioData = elevenLabsClient.textToSpeech(text, voiceId, settings);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Estimation de la durée (environ 150 mots par minute)
            int wordCount = text.split("\\s+").length;
            int estimatedDuration = (int) Math.ceil((wordCount / 150.0) * 60);
            
            SynthesisResult result = SynthesisResult.builder()
                    .audioData(audioData)
                    .durationSeconds(estimatedDuration)
                    .fileSizeBytes((long) audioData.length)
                    .processingTimeMs(processingTime)
                    .build();
            
            log.info("Synthèse réussie : durée estimée={}s, taille={}KB", 
                    estimatedDuration, audioData.length / 1024);
            
            return result;
            
        } catch (Exception e) {
            log.error("Erreur lors de la synthèse vocale", e);
            throw new RuntimeException("Échec de la synthèse vocale", e);
        }
    }
    
    /**
     * Synthétise avec des paramètres par défaut.
     * 
     * @param text Texte à synthétiser
     * @param voiceId ID de la voix
     * @return Résultat de la synthèse
     */
    public SynthesisResult synthesize(String text, String voiceId) {
        VoiceSettings defaultSettings = VoiceSettings.builder().build();
        return synthesize(text, voiceId, defaultSettings);
    }
}

package com.nexusai.audio.tts.service;

import com.nexusai.audio.tts.model.SynthesisResult;
import com.nexusai.audio.tts.model.VoiceSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Factory pour sélectionner le service TTS approprié.
 * 
 * <p>Permet de basculer facilement entre différents fournisseurs TTS
 * (ElevenLabs, Coqui, Google TTS, etc.)</p>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TTSServiceFactory {
    
    private final ElevenLabsTTSService elevenLabsService;
    
    /**
     * Synthétise du texte en utilisant le fournisseur spécifié.
     * 
     * @param text Texte à synthétiser
     * @param voiceId ID de la voix
     * @param provider Fournisseur TTS (ELEVENLABS, COQUI, etc.)
     * @param settings Paramètres vocaux
     * @return Résultat de la synthèse
     */
    public SynthesisResult synthesize(
            String text,
            String voiceId,
            String provider,
            VoiceSettings settings) {
        
        log.debug("Synthèse TTS avec provider: {}", provider);
        
        return switch (provider.toUpperCase()) {
            case "ELEVENLABS" -> elevenLabsService.synthesize(text, voiceId, settings);
            // case "COQUI" -> coquiService.synthesize(text, voiceId, settings);
            // case "GOOGLE" -> googleTTSService.synthesize(text, voiceId, settings);
            default -> {
                log.warn("Provider inconnu: {}, utilisation d'ElevenLabs par défaut", provider);
                yield elevenLabsService.synthesize(text, voiceId, settings);
            }
        };
    }
}
