package com.nexusai.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analytics_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_category", nullable = false, length = 50)
    private String eventCategory;

    @Column(name = "event_action", length = 100)
    private String eventAction;

    @Column(name = "event_label", length = 255)
    private String eventLabel;

    @Column(name = "event_value")
    private Double eventValue;

    @Column(columnDefinition = "jsonb")
    private String properties;

    @Column(name = "device_info", columnDefinition = "jsonb")
    private String deviceInfo;

    @Column(name = "geo_info", columnDefinition = "jsonb")
    private String geoInfo;

    @Column(length = 500)
    private String referrer;

    @Column(name = "page_url", length = 500)
    private String pageUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
