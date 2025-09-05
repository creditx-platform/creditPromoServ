package com.creditx.promo.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.creditx.promo.messaging.OutboxStreamPublisher;
import com.creditx.promo.model.OutboxEvent;
import com.creditx.promo.service.OutboxEventService;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublishingSchedulerTest {

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private OutboxStreamPublisher outboxStreamPublisher;

    @InjectMocks
    private OutboxEventPublishingScheduler outboxEventPublishingScheduler;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(outboxEventPublishingScheduler, "batchSize", 10);
    }

    @Test
    void shouldPublishPendingEvents() {
        OutboxEvent event1 = createOutboxEvent(123L, "{\"transactionId\":123}");
        OutboxEvent event2 = createOutboxEvent(456L, "{\"transactionId\":456}");
        List<OutboxEvent> events = Arrays.asList(event1, event2);
        when(outboxEventService.fetchPendingEvents(10)).thenReturn(events);
        outboxEventPublishingScheduler.publishPendingEvents();
        verify(outboxStreamPublisher, times(1)).publish("123", "{\"transactionId\":123}", "transaction.posted");
        verify(outboxStreamPublisher, times(1)).publish("456", "{\"transactionId\":456}", "transaction.posted");
        verify(outboxEventService, times(1)).markAsPublished(event1);
        verify(outboxEventService, times(1)).markAsPublished(event2);
    }

    @Test
    void shouldNotPublishWhenNoPendingEvents() {
        when(outboxEventService.fetchPendingEvents(10)).thenReturn(Collections.emptyList());
        outboxEventPublishingScheduler.publishPendingEvents();
        verify(outboxStreamPublisher, never()).publish(any(), any(), any());
        verify(outboxEventService, never()).markAsPublished(any());
        verify(outboxEventService, never()).markAsFailed(any());
    }

    @Test
    void shouldMarkAsFailedWhenPublishingFails() {
        OutboxEvent event = createOutboxEvent(123L, "{\"transactionId\":123}");
        when(outboxEventService.fetchPendingEvents(10)).thenReturn(Arrays.asList(event));
        doThrow(new RuntimeException("Publishing failed")).when(outboxStreamPublisher)
                .publish("123", "{\"transactionId\":123}", "transaction.posted");
        outboxEventPublishingScheduler.publishPendingEvents();
        verify(outboxStreamPublisher, times(1)).publish("123", "{\"transactionId\":123}", "transaction.posted");
        verify(outboxEventService, never()).markAsPublished(event);
        verify(outboxEventService, times(1)).markAsFailed(event);
    }

    private OutboxEvent createOutboxEvent(Long aggregateId, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(aggregateId);
        event.setPayload(payload);
        event.setEventType("transaction.posted");
        return event;
    }
}
