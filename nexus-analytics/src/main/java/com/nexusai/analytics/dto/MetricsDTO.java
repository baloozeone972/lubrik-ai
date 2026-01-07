package com.nexusai.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for analytics metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsDTO {

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // User metrics (for individual user reports)
    private Integer totalEvents;
    private Map<String, Long> eventCounts;
    private Integer totalMessages;
    private Integer totalTokensUsed;
    private Double avgMessagesPerDay;

    // Platform metrics (for global reports)
    private Long totalUsers;
    private Long activeUsers;
    private Long newUsers;

    // Engagement metrics
    private Long totalConversations;
    private Double avgMessagesPerUser;
    private Double avgConversationDuration;

    // Platform distribution
    private Map<String, Long> eventsByType;
    private Map<String, Long> eventsByCategory;
    private Map<String, Long> usersByPlatform;
    private Map<String, Long> usersByDevice;

    // Conversion metrics
    private Long totalSubscriptions;
    private Long totalRevenue;
    private Double conversionRate;

    // Performance metrics
    private Double avgResponseTime;
    private Long totalErrors;
    private Double errorRate;
}
