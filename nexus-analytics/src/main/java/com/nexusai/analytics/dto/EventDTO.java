package com.nexusai.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for tracking analytics events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDTO {
    private UUID userId;
    private String sessionId;
    private String eventType;
    private String eventCategory;
    private String eventAction;
    private String eventLabel;
    private Double eventValue;
    private Map<String, Object> eventData;
    private String ipAddress;
    private String userAgent;
    private String platform;
    private String deviceType;
    private LocalDateTime timestamp;
}
