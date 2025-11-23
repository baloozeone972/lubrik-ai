package com.nexusai.api.security.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String AUDIT_TOPIC = "audit-events";

    public void logEvent(AuditEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(AUDIT_TOPIC, event.eventType(), eventJson);
            log.debug("Audit event logged: {}", event.eventType());
        } catch (Exception e) {
            log.error("Failed to log audit event", e);
        }
    }

    public void logUserAction(UUID userId, String action, String resource, String details) {
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                "USER_ACTION",
                userId,
                action,
                resource,
                details,
                null,
                LocalDateTime.now()
        );
        logEvent(event);
    }

    public void logSecurityEvent(String eventType, String details, String ipAddress) {
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                eventType,
                null,
                eventType,
                null,
                details,
                Map.of("ipAddress", ipAddress),
                LocalDateTime.now()
        );
        logEvent(event);
    }

    public void logLogin(UUID userId, String ipAddress, boolean success) {
        String eventType = success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE";
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                eventType,
                userId,
                "LOGIN",
                "user:" + userId,
                success ? "Successful login" : "Failed login attempt",
                Map.of("ipAddress", ipAddress),
                LocalDateTime.now()
        );
        logEvent(event);
    }

    public void logDataAccess(UUID userId, String resource, String resourceId, String action) {
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                "DATA_ACCESS",
                userId,
                action,
                resource + ":" + resourceId,
                "User accessed " + resource,
                null,
                LocalDateTime.now()
        );
        logEvent(event);
    }

    public void logAdminAction(UUID adminId, String action, String targetType, String targetId, String reason) {
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                "ADMIN_ACTION",
                adminId,
                action,
                targetType + ":" + targetId,
                reason,
                Map.of("targetType", targetType, "targetId", targetId),
                LocalDateTime.now()
        );
        logEvent(event);
    }

    public void logError(UUID userId, String operation, String errorMessage, String stackTrace) {
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                "ERROR",
                userId,
                operation,
                null,
                errorMessage,
                Map.of("stackTrace", stackTrace != null ? stackTrace.substring(0, Math.min(1000, stackTrace.length())) : ""),
                LocalDateTime.now()
        );
        logEvent(event);
    }

    public record AuditEvent(
            UUID id,
            String eventType,
            UUID userId,
            String action,
            String resource,
            String details,
            Map<String, Object> metadata,
            LocalDateTime timestamp
    ) {}
}
