package com.nexusai.analytics.repository;

import com.nexusai.analytics.entity.DailyMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyMetricRepository extends JpaRepository<DailyMetric, UUID> {

    Optional<DailyMetric> findByMetricDateAndMetricTypeAndMetricName(
            LocalDate date, String metricType, String metricName);

    List<DailyMetric> findByMetricTypeAndMetricNameAndMetricDateBetween(
            String metricType, String metricName, LocalDate from, LocalDate to);

    List<DailyMetric> findByMetricDateOrderByMetricTypeAscMetricNameAsc(LocalDate date);
}
