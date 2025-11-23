package com.nexusai.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricSummary {
    private LocalDate date;
    private String metricType;
    private String metricName;
    private Long totalCount;
    private Double totalValue;
    private Double averageValue;
    private Integer uniqueUsers;
    private Map<String, Object> breakdown;
}
