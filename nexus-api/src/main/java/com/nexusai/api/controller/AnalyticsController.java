package com.nexusai.api.controller;

import com.nexusai.analytics.dto.*;
import com.nexusai.analytics.service.EventService;
import com.nexusai.analytics.service.MetricService;
import com.nexusai.auth.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Analytics and metrics endpoints")
public class AnalyticsController {

    private final EventService eventService;
    private final MetricService metricService;

    // Event tracking

    @PostMapping("/events")
    @Operation(summary = "Track an event")
    public ResponseEntity<AnalyticsEventDTO> trackEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TrackEventRequest request) {
        AnalyticsEventDTO event = eventService.trackEvent(principal.getUserId(), request);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/events")
    @Operation(summary = "Get user's events")
    public ResponseEntity<Page<AnalyticsEventDTO>> getUserEvents(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        Page<AnalyticsEventDTO> events = eventService.getUserEvents(principal.getUserId(), pageable);
        return ResponseEntity.ok(events);
    }

    // User activity

    @GetMapping("/activity")
    @Operation(summary = "Get user's activity summary")
    public ResponseEntity<UserActivitySummary> getUserActivity(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        UserActivitySummary activity = metricService.getUserActivity(principal.getUserId(), targetDate);
        return ResponseEntity.ok(activity);
    }

    @GetMapping("/activity/range")
    @Operation(summary = "Get user's activity for date range")
    public ResponseEntity<List<UserActivitySummary>> getUserActivityRange(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<UserActivitySummary> activities = metricService.getUserActivityRange(
                principal.getUserId(), from, to);
        return ResponseEntity.ok(activities);
    }

    // User stats

    @GetMapping("/stats")
    @Operation(summary = "Get user's statistics")
    public ResponseEntity<Map<String, Object>> getUserStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        UserActivitySummary todayActivity = metricService.getUserActivity(principal.getUserId(), today);
        List<UserActivitySummary> weekActivity = metricService.getUserActivityRange(
                principal.getUserId(), weekAgo, today);

        int totalMessages = weekActivity.stream()
                .mapToInt(a -> a.getMessagesSent() + a.getMessagesReceived())
                .sum();

        int totalTokens = weekActivity.stream()
                .mapToInt(UserActivitySummary::getTokensUsed)
                .sum();

        return ResponseEntity.ok(Map.of(
                "today", todayActivity != null ? todayActivity : Map.of(),
                "weeklyMessages", totalMessages,
                "weeklyTokens", totalTokens,
                "weeklyActivity", weekActivity
        ));
    }

    // Admin endpoints

    @GetMapping("/admin/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get platform metrics (admin only)")
    public ResponseEntity<Map<String, Object>> getPlatformStats() {
        Map<String, Object> stats = metricService.getPlatformStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/admin/metrics/{metricType}/{metricName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get specific metric (admin only)")
    public ResponseEntity<MetricSummary> getMetric(
            @PathVariable String metricType,
            @PathVariable String metricName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        MetricSummary metric = metricService.getDailyMetric(targetDate, metricType, metricName);
        return ResponseEntity.ok(metric);
    }

    @GetMapping("/admin/metrics/{metricType}/{metricName}/range")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get metric for date range (admin only)")
    public ResponseEntity<List<MetricSummary>> getMetricRange(
            @PathVariable String metricType,
            @PathVariable String metricName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<MetricSummary> metrics = metricService.getMetricRange(metricType, metricName, from, to);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/admin/events/counts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get event counts by type (admin only)")
    public ResponseEntity<List<Map<String, Object>>> getEventCounts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<Map<String, Object>> counts = eventService.getEventCounts(from, to);
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/admin/events/realtime/{eventType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get realtime event count (admin only)")
    public ResponseEntity<Map<String, Object>> getRealtimeCount(@PathVariable String eventType) {
        long count = eventService.getRealtimeEventCount(eventType);
        return ResponseEntity.ok(Map.of(
                "eventType", eventType,
                "count", count,
                "timestamp", LocalDateTime.now()
        ));
    }
}
