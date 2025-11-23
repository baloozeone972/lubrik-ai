package com.nexusai.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackEventRequest {

    @NotBlank(message = "Event type is required")
    private String eventType;

    @NotBlank(message = "Event category is required")
    private String eventCategory;

    private String eventAction;
    private String eventLabel;
    private Double eventValue;
    private Map<String, Object> properties;
    private String sessionId;
}
