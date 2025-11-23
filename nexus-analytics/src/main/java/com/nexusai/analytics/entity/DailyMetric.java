package com.nexusai.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "metric_type", nullable = false, length = 100)
    private String metricType;

    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    @Column(length = 100)
    private String dimension;

    @Column(name = "dimension_value", length = 255)
    private String dimensionValue;

    @Column(name = "value_count")
    @Builder.Default
    private Long valueCount = 0L;

    @Column(name = "value_sum", precision = 20, scale = 4)
    @Builder.Default
    private BigDecimal valueSum = BigDecimal.ZERO;

    @Column(name = "value_avg", precision = 20, scale = 4)
    @Builder.Default
    private BigDecimal valueAvg = BigDecimal.ZERO;

    @Column(name = "value_min", precision = 20, scale = 4)
    private BigDecimal valueMin;

    @Column(name = "value_max", precision = 20, scale = 4)
    private BigDecimal valueMax;

    @Column(name = "unique_users")
    @Builder.Default
    private Integer uniqueUsers = 0;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
