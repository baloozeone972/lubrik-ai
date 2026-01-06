package com.nexusai.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant un événement analytics.
 */
@Entity
@Table(name = "analytics_events", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_user_timestamp", columnList = "user_id,timestamp")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_category")
    private String eventCategory;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData; // JSON

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "platform")
    private String platform;

    @Column(name = "device_type")
    private String deviceType;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // Helper methods
    public boolean isMessageEvent() {
        return "message_sent".equals(eventType) || "message_received".equals(eventType);
    }

    public boolean isConversionEvent() {
        return "subscription_created".equals(eventType) || "payment_completed".equals(eventType);
    }
}
