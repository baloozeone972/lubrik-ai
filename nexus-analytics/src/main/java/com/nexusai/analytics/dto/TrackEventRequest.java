package com.nexusai.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for tracking an event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackEventRequest {

    private String sessionId;
    private String eventType;
    private String eventCategory;
    private String eventAction;
    private String eventLabel;
    private Double eventValue;
    private Map<String, Object> properties;
}
