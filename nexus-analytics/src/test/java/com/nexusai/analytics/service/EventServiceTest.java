package com.nexusai.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusai.analytics.dto.AnalyticsEventDTO;
import com.nexusai.analytics.dto.TrackEventRequest;
import com.nexusai.analytics.entity.AnalyticsEvent;
import com.nexusai.analytics.repository.AnalyticsEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Tests")
class EventServiceTest {

    @Mock
    private AnalyticsEventRepository eventRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private EventService eventService;

    private UUID userId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        eventService = new EventService(eventRepository, redisTemplate, kafkaTemplate, objectMapper);

        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("TrackEvent Tests")
    class TrackEventTests {

        @Test
        @DisplayName("Should track event successfully")
        void shouldTrackEventSuccessfully() {
            TrackEventRequest request = TrackEventRequest.builder()
                    .eventType("page_view")
                    .eventCategory("navigation")
                    .eventAction("view")
                    .eventLabel("home")
                    .sessionId(sessionId)
                    .build();

            AnalyticsEvent savedEvent = AnalyticsEvent.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .eventType("page_view")
                    .eventCategory("navigation")
                    .build();

            when(eventRepository.save(any(AnalyticsEvent.class))).thenReturn(savedEvent);

            AnalyticsEventDTO result = eventService.trackEvent(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getEventType()).isEqualTo("page_view");
            verify(eventRepository).save(any(AnalyticsEvent.class));
            verify(valueOperations).increment(anyString());
        }

        @Test
        @DisplayName("Should throw exception for null event type")
        void shouldThrowExceptionForNullEventType() {
            TrackEventRequest request = TrackEventRequest.builder()
                    .eventCategory("navigation")
                    .build();

            assertThatThrownBy(() -> eventService.trackEvent(userId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Event type is required");
        }

        @Test
        @DisplayName("Should throw exception for null event category")
        void shouldThrowExceptionForNullEventCategory() {
            TrackEventRequest request = TrackEventRequest.builder()
                    .eventType("page_view")
                    .build();

            assertThatThrownBy(() -> eventService.trackEvent(userId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Event category is required");
        }

        @Test
        @DisplayName("Should throw exception for invalid event type format")
        void shouldThrowExceptionForInvalidEventTypeFormat() {
            TrackEventRequest request = TrackEventRequest.builder()
                    .eventType("invalid type with spaces")
                    .eventCategory("navigation")
                    .build();

            assertThatThrownBy(() -> eventService.trackEvent(userId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid event type format");
        }

        @Test
        @DisplayName("Should serialize properties correctly")
        void shouldSerializePropertiesCorrectly() {
            TrackEventRequest request = TrackEventRequest.builder()
                    .eventType("click")
                    .eventCategory("button")
                    .properties(Map.of("button_id", "submit", "page", "checkout"))
                    .build();

            AnalyticsEvent savedEvent = AnalyticsEvent.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .eventType("click")
                    .build();

            when(eventRepository.save(any(AnalyticsEvent.class))).thenReturn(savedEvent);

            eventService.trackEvent(userId, request);

            ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
            verify(eventRepository).save(captor.capture());
            assertThat(captor.getValue().getProperties()).contains("button_id");
        }
    }

    @Nested
    @DisplayName("GetUserEvents Tests")
    class GetUserEventsTests {

        @Test
        @DisplayName("Should return paginated user events")
        void shouldReturnPaginatedUserEvents() {
            Pageable pageable = PageRequest.of(0, 10);
            AnalyticsEvent event = AnalyticsEvent.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .eventType("page_view")
                    .eventCategory("navigation")
                    .build();
            Page<AnalyticsEvent> eventPage = new PageImpl<>(List.of(event), pageable, 1);

            when(eventRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(eventPage);

            Page<AnalyticsEventDTO> result = eventService.getUserEvents(userId, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getEventType()).isEqualTo("page_view");
        }

        @Test
        @DisplayName("Should return empty page when no events")
        void shouldReturnEmptyPageWhenNoEvents() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<AnalyticsEvent> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(eventRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(emptyPage);

            Page<AnalyticsEventDTO> result = eventService.getUserEvents(userId, pageable);

            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("GetEventsByType Tests")
    class GetEventsByTypeTests {

        @Test
        @DisplayName("Should return events by type within date range")
        void shouldReturnEventsByTypeWithinDateRange() {
            Pageable pageable = PageRequest.of(0, 10);
            LocalDateTime from = LocalDateTime.now().minusDays(7);
            LocalDateTime to = LocalDateTime.now();
            AnalyticsEvent event = AnalyticsEvent.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .eventType("purchase")
                    .eventCategory("commerce")
                    .build();
            Page<AnalyticsEvent> eventPage = new PageImpl<>(List.of(event), pageable, 1);

            when(eventRepository.findByEventTypeAndCreatedAtBetween("purchase", from, to, pageable))
                    .thenReturn(eventPage);

            Page<AnalyticsEventDTO> result = eventService.getEventsByType("purchase", from, to, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getEventCategory()).isEqualTo("commerce");
        }
    }

    @Nested
    @DisplayName("GetRealtimeEventCount Tests")
    class GetRealtimeEventCountTests {

        @Test
        @DisplayName("Should return event count from Redis")
        void shouldReturnEventCountFromRedis() {
            when(valueOperations.get(anyString())).thenReturn("42");

            long count = eventService.getRealtimeEventCount("page_view");

            assertThat(count).isEqualTo(42);
        }

        @Test
        @DisplayName("Should return zero when no count in Redis")
        void shouldReturnZeroWhenNoCount() {
            when(valueOperations.get(anyString())).thenReturn(null);

            long count = eventService.getRealtimeEventCount("page_view");

            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("TrackEventAsync Tests")
    class TrackEventAsyncTests {

        @Test
        @DisplayName("Should publish event to Kafka")
        void shouldPublishEventToKafka() {
            eventService.trackEventAsync(userId, "click", "button", Map.of("id", "submit"));

            verify(kafkaTemplate).send(eq("analytics-events"), anyString());
        }

        @Test
        @DisplayName("Should handle Kafka publish failure gracefully")
        void shouldHandleKafkaFailureGracefully() {
            when(kafkaTemplate.send(anyString(), anyString())).thenThrow(new RuntimeException("Kafka error"));

            // Should not throw
            assertThatCode(() -> eventService.trackEventAsync(userId, "click", "button", null))
                    .doesNotThrowAnyException();
        }
    }
}
