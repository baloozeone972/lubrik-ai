package com.nexusai.analytics.service;

import com.nexusai.analytics.dto.MetricSummary;
import com.nexusai.analytics.dto.UserActivitySummary;
import com.nexusai.analytics.entity.DailyMetric;
import com.nexusai.analytics.repository.DailyMetricRepository;
import com.nexusai.analytics.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricService {

    private final DailyMetricRepository dailyMetricRepository;
    private final UserActivityRepository userActivityRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String METRIC_CACHE_PREFIX = "metrics:";
    private static final double ANOMALY_THRESHOLD = 2.0; // Standard deviations

    @Transactional(readOnly = true)
    public MetricSummary getDailyMetric(LocalDate date, String metricType, String metricName) {
        return dailyMetricRepository.findByMetricDateAndMetricTypeAndMetricName(date, metricType, metricName)
                .map(this::mapToSummary)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<MetricSummary> getMetricRange(String metricType, String metricName,
                                               LocalDate from, LocalDate to) {
        return dailyMetricRepository.findByMetricTypeAndMetricNameAndMetricDateBetween(
                        metricType, metricName, from, to)
                .stream()
                .map(this::mapToSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserActivitySummary getUserActivity(UUID userId, LocalDate date) {
        return userActivityRepository.findByUserIdAndSummaryDate(userId, date)
                .map(entity -> UserActivitySummary.builder()
                        .userId(entity.getUserId())
                        .date(entity.getSummaryDate())
                        .conversationsStarted(entity.getConversationsStarted())
                        .messagesSent(entity.getMessagesSent())
                        .messagesReceived(entity.getMessagesReceived())
                        .tokensUsed(entity.getTokensUsed())
                        .companionsCreated(entity.getCompanionsCreated())
                        .timeSpentSeconds(entity.getTimeSpentSeconds())
                        .mostUsedCompanionId(entity.getMostUsedCompanionId())
                        .build())
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<UserActivitySummary> getUserActivityRange(UUID userId, LocalDate from, LocalDate to) {
        return userActivityRepository.findByUserIdAndSummaryDateBetween(userId, from, to)
                .stream()
                .map(entity -> UserActivitySummary.builder()
                        .userId(entity.getUserId())
                        .date(entity.getSummaryDate())
                        .conversationsStarted(entity.getConversationsStarted())
                        .messagesSent(entity.getMessagesSent())
                        .messagesReceived(entity.getMessagesReceived())
                        .tokensUsed(entity.getTokensUsed())
                        .build())
                .toList();
    }

    public Map<String, Object> getPlatformStats() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        Map<String, Object> stats = new HashMap<>();

        // Get cached stats or compute
        String cacheKey = METRIC_CACHE_PREFIX + "platform:" + today;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            // Return cached stats
            stats.put("cached", true);
        }

        stats.put("date", today);
        stats.put("activeUsers", getActiveUserCount(today));
        stats.put("totalConversations", getTotalConversations(today));
        stats.put("totalMessages", getTotalMessages(today));
        stats.put("averageSessionDuration", getAverageSessionDuration(today));

        return stats;
    }

    public boolean detectAnomaly(String metricType, String metricName, double currentValue) {
        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusDays(30);

        List<DailyMetric> historicalMetrics = dailyMetricRepository
                .findByMetricTypeAndMetricNameAndMetricDateBetween(metricType, metricName, monthAgo, today.minusDays(1));

        if (historicalMetrics.size() < 7) {
            return false; // Not enough data
        }

        double mean = historicalMetrics.stream()
                .mapToDouble(m -> m.getValueSum().doubleValue())
                .average()
                .orElse(0);

        double stdDev = Math.sqrt(historicalMetrics.stream()
                .mapToDouble(m -> Math.pow(m.getValueSum().doubleValue() - mean, 2))
                .average()
                .orElse(0));

        if (stdDev == 0) return false;

        double zScore = Math.abs(currentValue - mean) / stdDev;
        boolean isAnomaly = zScore > ANOMALY_THRESHOLD;

        if (isAnomaly) {
            log.warn("Anomaly detected for {}/{}: value={}, mean={}, stdDev={}, zScore={}",
                    metricType, metricName, currentValue, mean, stdDev, zScore);
        }

        return isAnomaly;
    }

    @Scheduled(cron = "0 0 1 * * ?") // Run at 1 AM daily
    @Transactional
    public void aggregateDailyMetrics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Starting daily metrics aggregation for {}", yesterday);

        // Aggregate various metrics
        aggregateUserMetrics(yesterday);
        aggregateConversationMetrics(yesterday);
        aggregateCompanionMetrics(yesterday);

        log.info("Completed daily metrics aggregation for {}", yesterday);
    }

    private void aggregateUserMetrics(LocalDate date) {
        // Would aggregate from analytics_events table
        log.debug("Aggregating user metrics for {}", date);
    }

    private void aggregateConversationMetrics(LocalDate date) {
        // Would aggregate from conversations table
        log.debug("Aggregating conversation metrics for {}", date);
    }

    private void aggregateCompanionMetrics(LocalDate date) {
        // Would aggregate from companion_analytics table
        log.debug("Aggregating companion metrics for {}", date);
    }

    private long getActiveUserCount(LocalDate date) {
        return dailyMetricRepository.findByMetricDateAndMetricTypeAndMetricName(
                        date, "users", "active_count")
                .map(m -> m.getValueCount())
                .orElse(0L);
    }

    private long getTotalConversations(LocalDate date) {
        return dailyMetricRepository.findByMetricDateAndMetricTypeAndMetricName(
                        date, "conversations", "total_count")
                .map(m -> m.getValueCount())
                .orElse(0L);
    }

    private long getTotalMessages(LocalDate date) {
        return dailyMetricRepository.findByMetricDateAndMetricTypeAndMetricName(
                        date, "messages", "total_count")
                .map(m -> m.getValueCount())
                .orElse(0L);
    }

    private double getAverageSessionDuration(LocalDate date) {
        return dailyMetricRepository.findByMetricDateAndMetricTypeAndMetricName(
                        date, "sessions", "avg_duration")
                .map(m -> m.getValueAvg().doubleValue())
                .orElse(0.0);
    }

    private MetricSummary mapToSummary(DailyMetric metric) {
        return MetricSummary.builder()
                .date(metric.getMetricDate())
                .metricType(metric.getMetricType())
                .metricName(metric.getMetricName())
                .totalCount(metric.getValueCount())
                .totalValue(metric.getValueSum().doubleValue())
                .averageValue(metric.getValueAvg().doubleValue())
                .uniqueUsers(metric.getUniqueUsers())
                .build();
    }
}
