package com.nexusai.analytics.core.service;

import com.nexusai.analytics.core.model.*;
import com.nexusai.analytics.core.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ═══════════════════════════════════════════════════════════════
 * ANALYTICS - TESTS UNITAIRES
 * 
 * Tests unitaires pour les services du module Analytics.
 * 
 * Framework : JUnit 5 + Mockito + AssertJ
 * Coverage : 80%+ requis
 * ═══════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════
// 1. EVENT SERVICE TESTS
// ═══════════════════════════════════════════════════════════════

/**
 * Tests unitaires pour EventService.
 * 
 * @author Équipe Analytics - Sous-équipe Tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Tests")
class EventServiceTest {
    
    @Mock
    private EventRepository eventRepository;
    
    @InjectMocks
    private EventService eventService;
    
    private UserEvent sampleEvent;
    
    @BeforeEach
    void setUp() {
        // Préparer un événement de test
        sampleEvent = UserEvent.builder()
            .userId(UUID.randomUUID())
            .eventType("page_view")
            .eventData(Map.of("page", "/home"))
            .timestamp(Instant.now())
            .deviceType("desktop")
            .platform("web")
            .build();
    }
    
    @Test
    @DisplayName("Should record event successfully")
    void shouldRecordEventSuccessfully() {
        // Given
        when(eventRepository.save(any(UserEvent.class)))
            .thenReturn(sampleEvent);
        
        // When
        UserEvent result = eventService.recordEvent(sampleEvent);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEventId()).isNotNull();
        assertThat(result.getTimestamp()).isNotNull();
        
        verify(eventRepository, times(1)).save(any(UserEvent.class));
    }
    
    @Test
    @DisplayName("Should throw exception when userId is null")
    void shouldThrowExceptionWhenUserIdIsNull() {
        // Given
        UserEvent invalidEvent = UserEvent.builder()
            .eventType("test")
            .build();
        
        // When & Then
        assertThatThrownBy(() -> eventService.recordEvent(invalidEvent))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("User ID cannot be null");
        
        verify(eventRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should throw exception when eventType is null")
    void shouldThrowExceptionWhenEventTypeIsNull() {
        // Given
        UserEvent invalidEvent = UserEvent.builder()
            .userId(UUID.randomUUID())
            .build();
        
        // When & Then
        assertThatThrownBy(() -> eventService.recordEvent(invalidEvent))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Event type cannot be null");
    }
    
    @Test
    @DisplayName("Should throw exception when eventType is empty")
    void shouldThrowExceptionWhenEventTypeIsEmpty() {
        // Given
        UserEvent invalidEvent = UserEvent.builder()
            .userId(UUID.randomUUID())
            .eventType("")
            .build();
        
        // When & Then
        assertThatThrownBy(() -> eventService.recordEvent(invalidEvent))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Event type cannot be empty");
    }
    
    @Test
    @DisplayName("Should record batch of events successfully")
    void shouldRecordBatchOfEventsSuccessfully() {
        // Given
        List<UserEvent> events = Arrays.asList(
            sampleEvent,
            UserEvent.builder()
                .userId(UUID.randomUUID())
                .eventType("button_click")
                .build()
        );
        
        when(eventRepository.saveBatch(anyList())).thenReturn(2);
        
        // When
        int count = eventService.recordEventsBatch(events);
        
        // Then
        assertThat(count).isEqualTo(2);
        verify(eventRepository, times(1)).saveBatch(anyList());
    }
    
    @Test
    @DisplayName("Should get user events for period")
    void shouldGetUserEventsForPeriod() {
        // Given
        UUID userId = UUID.randomUUID();
        Instant startTime = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant endTime = Instant.now();
        
        List<UserEvent> expectedEvents = Arrays.asList(sampleEvent);
        
        when(eventRepository.findByUserIdAndTimestampBetween(userId, startTime, endTime))
            .thenReturn(expectedEvents);
        
        // When
        List<UserEvent> result = eventService.getUserEvents(userId, startTime, endTime);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result).isEqualTo(expectedEvents);
    }
    
    @Test
    @DisplayName("Should count user events")
    void shouldCountUserEvents() {
        // Given
        UUID userId = UUID.randomUUID();
        Instant startTime = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant endTime = Instant.now();
        
        when(eventRepository.countByUserIdAndTimestampBetween(userId, startTime, endTime))
            .thenReturn(42L);
        
        // When
        long count = eventService.countUserEvents(userId, startTime, endTime);
        
        // Then
        assertThat(count).isEqualTo(42L);
    }
    
    @Test
    @DisplayName("Should get top events")
    void shouldGetTopEvents() {
        // Given
        Instant startTime = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant endTime = Instant.now();
        int limit = 10;
        
        Map<String, Long> expectedTopEvents = Map.of(
            "page_view", 1000L,
            "button_click", 500L,
            "form_submit", 200L
        );
        
        when(eventRepository.findTopEventTypes(startTime, endTime, limit))
            .thenReturn(expectedTopEvents);
        
        // When
        Map<String, Long> result = eventService.getTopEvents(startTime, endTime, limit);
        
        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get("page_view")).isEqualTo(1000L);
    }
}

// ═══════════════════════════════════════════════════════════════
// 2. METRIC SERVICE TESTS
// ═══════════════════════════════════════════════════════════════

/**
 * Tests unitaires pour MetricService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MetricService Tests")
class MetricServiceTest {
    
    @Mock
    private MetricRepository metricRepository;
    
    @InjectMocks
    private MetricService metricService;
    
    private SystemMetric sampleMetric;
    
    @BeforeEach
    void setUp() {
        sampleMetric = SystemMetric.builder()
            .metricName("cpu_usage")
            .metricValue(45.2)
            .serviceName("user-service")
            .timestamp(Instant.now())
            .tags(Map.of("instance", "1"))
            .build();
    }
    
    @Test
    @DisplayName("Should record metric successfully")
    void shouldRecordMetricSuccessfully() {
        // Given
        when(metricRepository.save(any(SystemMetric.class)))
            .thenReturn(sampleMetric);
        
        // When
        SystemMetric result = metricService.recordMetric(sampleMetric);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMetricName()).isEqualTo("cpu_usage");
        assertThat(result.getMetricValue()).isEqualTo(45.2);
    }
    
    @Test
    @DisplayName("Should set timestamp if not provided")
    void shouldSetTimestampIfNotProvided() {
        // Given
        SystemMetric metricWithoutTimestamp = SystemMetric.builder()
            .metricName("memory_usage")
            .metricValue(60.0)
            .serviceName("user-service")
            .build();
        
        when(metricRepository.save(any(SystemMetric.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        SystemMetric result = metricService.recordMetric(metricWithoutTimestamp);
        
        // Then
        assertThat(result.getTimestamp()).isNotNull();
        assertThat(result.getTimestamp()).isBeforeOrEqualTo(Instant.now());
    }
    
    @Test
    @DisplayName("Should record batch of metrics successfully")
    void shouldRecordBatchOfMetricsSuccessfully() {
        // Given
        List<SystemMetric> metrics = Arrays.asList(
            sampleMetric,
            SystemMetric.builder()
                .metricName("memory_usage")
                .metricValue(60.0)
                .serviceName("user-service")
                .build()
        );
        
        when(metricRepository.saveBatch(anyList())).thenReturn(2);
        
        // When
        int count = metricService.recordMetricsBatch(metrics);
        
        // Then
        assertThat(count).isEqualTo(2);
    }
    
    @Test
    @DisplayName("Should get service metrics")
    void shouldGetServiceMetrics() {
        // Given
        String serviceName = "user-service";
        Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endTime = Instant.now();
        
        List<SystemMetric> expectedMetrics = Arrays.asList(sampleMetric);
        
        when(metricRepository.findByServiceNameAndTimestampBetween(
            serviceName, startTime, endTime))
            .thenReturn(expectedMetrics);
        
        // When
        List<SystemMetric> result = metricService.getServiceMetrics(
            serviceName, startTime, endTime);
        
        // Then
        assertThat(result).hasSize(1);
    }
    
    @Test
    @DisplayName("Should check if metric is above threshold")
    void shouldCheckIfMetricIsAboveThreshold() {
        // Given
        String metricName = "cpu_usage";
        double threshold = 80.0;
        long duration = 5; // minutes
        
        MetricStatistics stats = MetricStatistics.builder()
            .avgValue(85.0)
            .build();
        
        when(metricRepository.calculateStatistics(
            eq(metricName), any(Instant.class), any(Instant.class)))
            .thenReturn(stats);
        
        // When
        boolean result = metricService.isMetricAboveThreshold(
            metricName, threshold, duration);
        
        // Then
        assertThat(result).isTrue();
    }
}

// ═══════════════════════════════════════════════════════════════
// 3. AGGREGATION SERVICE TESTS
// ═══════════════════════════════════════════════════════════════

/**
 * Tests unitaires pour AggregationService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AggregationService Tests")
class AggregationServiceTest {
    
    @Mock
    private EventRepository eventRepository;
    
    @Mock
    private MetricRepository metricRepository;
    
    @Mock
    private AggregatedMetricRepository aggregatedMetricRepository;
    
    @InjectMocks
    private AggregationService aggregationService;
    
    @Test
    @DisplayName("Should aggregate events for period")
    void shouldAggregateEventsForPeriod() {
        // Given
        AggregationPeriod period = AggregationPeriod.HOUR;
        Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endTime = Instant.now();
        
        Map<String, Long> eventCounts = Map.of(
            "page_view", 1000L,
            "button_click", 500L
        );
        
        when(eventRepository.findTopEventTypes(startTime, endTime, Integer.MAX_VALUE))
            .thenReturn(eventCounts);
        
        // When
        aggregationService.aggregateEventsForPeriod(period, startTime, endTime);
        
        // Then
        verify(aggregatedMetricRepository, times(2)).save(any(AggregatedMetric.class));
    }
}

// ═══════════════════════════════════════════════════════════════
// 4. INTEGRATION TESTS - Tests avec TestContainers
// ═══════════════════════════════════════════════════════════════

/**
 * Tests d'intégration avec ClickHouse réel (TestContainers).
 */
@org.springframework.boot.test.context.SpringBootTest
@org.testcontainers.junit.jupiter.Testcontainers
@DisplayName("Integration Tests - ClickHouse")
class ClickHouseIntegrationTest {
    
    @org.testcontainers.junit.jupiter.Container
    static org.testcontainers.containers.ClickHouseContainer clickhouse = 
        new org.testcontainers.containers.ClickHouseContainer("clickhouse/clickhouse-server:23.8")
            .withExposedPorts(8123);
    
    @org.springframework.beans.factory.annotation.Autowired
    private EventService eventService;
    
    @Test
    @DisplayName("Should insert and retrieve event from ClickHouse")
    void shouldInsertAndRetrieveEventFromClickHouse() {
        // Given
        UUID userId = UUID.randomUUID();
        UserEvent event = UserEvent.builder()
            .userId(userId)
            .eventType("integration_test")
            .eventData(Map.of("test", "data"))
            .build();
        
        // When
        UserEvent savedEvent = eventService.recordEvent(event);
        
        // Then
        assertThat(savedEvent).isNotNull();
        assertThat(savedEvent.getEventId()).isNotNull();
        
        // Vérifier la récupération
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        
        List<UserEvent> events = eventService.getUserEvents(userId, yesterday, now);
        assertThat(events).isNotEmpty();
        assertThat(events.get(0).getEventType()).isEqualTo("integration_test");
    }
}

// ═══════════════════════════════════════════════════════════════
// 5. CONTROLLER TESTS - Tests des APIs REST
// ═══════════════════════════════════════════════════════════════

/**
 * Tests des controllers REST.
 */
@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(
    com.nexusai.analytics.api.controller.EventController.class
)
@DisplayName("EventController Tests")
class EventControllerTest {
    
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;
    
    @org.springframework.boot.test.mock.mockito.MockBean
    private EventService eventService;
    
    @Test
    @DisplayName("POST /api/v1/analytics/events should return 201")
    void postEventShouldReturn201() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UserEvent event = UserEvent.builder()
            .eventId(UUID.randomUUID())
            .userId(userId)
            .eventType("test_event")
            .timestamp(Instant.now())
            .build();
        
        when(eventService.recordEvent(any(UserEvent.class)))
            .thenReturn(event);
        
        String requestBody = String.format("""
            {
                "userId": "%s",
                "eventType": "test_event",
                "eventData": {}
            }
            """, userId);
        
        // When & Then
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/api/v1/analytics/events")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .status().isCreated())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .jsonPath("$.eventId").exists())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .jsonPath("$.eventType").value("test_event"));
    }
    
    @Test
    @DisplayName("GET /api/v1/analytics/events/user/{userId} should return events")
    void getUserEventsShouldReturnEvents() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UserEvent event = UserEvent.builder()
            .eventId(UUID.randomUUID())
            .userId(userId)
            .eventType("test_event")
            .timestamp(Instant.now())
            .build();
        
        when(eventService.getUserEvents(any(UUID.class), any(Instant.class), any(Instant.class)))
            .thenReturn(List.of(event));
        
        // When & Then
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/api/v1/analytics/events/user/" + userId)
            )
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .jsonPath("$").isArray())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .jsonPath("$[0].eventType").value("test_event"));
    }
}

// ═══════════════════════════════════════════════════════════════
// 6. PERFORMANCE TESTS - Tests de performance
// ═══════════════════════════════════════════════════════════════

/**
 * Tests de performance pour valider les exigences.
 */
@DisplayName("Performance Tests")
class PerformanceTest {
    
    private EventService eventService;
    private EventRepository eventRepository;
    
    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        eventService = new EventService(eventRepository);
    }
    
    @Test
    @DisplayName("Should handle 1000 events in less than 1 second")
    @Timeout(value = 1, unit = java.util.concurrent.TimeUnit.SECONDS)
    void shouldHandle1000EventsInLessThan1Second() {
        // Given
        List<UserEvent> events = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            events.add(UserEvent.builder()
                .userId(UUID.randomUUID())
                .eventType("perf_test")
                .build());
        }
        
        when(eventRepository.saveBatch(anyList())).thenReturn(1000);
        
        // When
        long startTime = System.currentTimeMillis();
        int count = eventService.recordEventsBatch(events);
        long duration = System.currentTimeMillis() - startTime;
        
        // Then
        assertThat(count).isEqualTo(1000);
        assertThat(duration).isLessThan(1000); // < 1 seconde
    }
}

// ═══════════════════════════════════════════════════════════════
// 7. TEST CONFIGURATION
// ═══════════════════════════════════════════════════════════════

/**
 * Configuration de test.
 */
@org.springframework.boot.test.context.TestConfiguration
class TestConfig {
    
    @org.springframework.context.annotation.Bean
    public EventRepository eventRepository() {
        return mock(EventRepository.class);
    }
    
    @org.springframework.context.annotation.Bean
    public MetricRepository metricRepository() {
        return mock(MetricRepository.class);
    }
}
