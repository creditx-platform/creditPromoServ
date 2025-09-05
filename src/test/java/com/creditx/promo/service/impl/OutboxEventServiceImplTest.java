package com.creditx.promo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.creditx.promo.model.OutboxEvent;
import com.creditx.promo.model.OutboxEventStatus;
import com.creditx.promo.repository.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceImplTest {

    @Mock
    private OutboxEventRepository repository;

    @InjectMocks
    private OutboxEventServiceImpl outboxEventServiceImpl;

    @Test
    void shouldSaveEvent() {
        String eventType = "transaction.posted";
        Long aggregateId = 123L;
        String payload = "{\"transactionId\":123}";
        OutboxEvent savedEvent = createOutboxEvent(eventType, aggregateId, payload, OutboxEventStatus.PENDING);
        when(repository.save(any(OutboxEvent.class))).thenReturn(savedEvent);
        OutboxEvent result = outboxEventServiceImpl.saveEvent(eventType, aggregateId, payload);
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository, times(1)).save(eventCaptor.capture());
        OutboxEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo(eventType);
        assertThat(capturedEvent.getAggregateId()).isEqualTo(aggregateId);
        assertThat(capturedEvent.getPayload()).isEqualTo(payload);
        assertThat(capturedEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(result).isEqualTo(savedEvent);
    }

    @Test
    void shouldFetchPendingEvents() {
        OutboxEvent pendingEvent1 = createOutboxEvent("EVENT_1", 123L, "{\"data\":1}", OutboxEventStatus.PENDING);
        OutboxEvent pendingEvent2 = createOutboxEvent("EVENT_2", 456L, "{\"data\":2}", OutboxEventStatus.PENDING);
        OutboxEvent publishedEvent = createOutboxEvent("EVENT_3", 789L, "{\"data\":3}", OutboxEventStatus.PUBLISHED);
        OutboxEvent failedEvent = createOutboxEvent("EVENT_4", 101L, "{\"data\":4}", OutboxEventStatus.FAILED);
        List<OutboxEvent> allEvents = Arrays.asList(pendingEvent1, pendingEvent2, publishedEvent, failedEvent);
        when(repository.findAll()).thenReturn(allEvents);
        List<OutboxEvent> result = outboxEventServiceImpl.fetchPendingEvents(5);
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(pendingEvent1, pendingEvent2);
        verify(repository, times(1)).findAll();
    }

    @Test
    void shouldLimitFetchedPendingEvents() {
        OutboxEvent pendingEvent1 = createOutboxEvent("EVENT_1", 123L, "{\"data\":1}", OutboxEventStatus.PENDING);
        OutboxEvent pendingEvent2 = createOutboxEvent("EVENT_2", 456L, "{\"data\":2}", OutboxEventStatus.PENDING);
        OutboxEvent pendingEvent3 = createOutboxEvent("EVENT_3", 789L, "{\"data\":3}", OutboxEventStatus.PENDING);
        when(repository.findAll()).thenReturn(Arrays.asList(pendingEvent1, pendingEvent2, pendingEvent3));
        List<OutboxEvent> result = outboxEventServiceImpl.fetchPendingEvents(2);
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(pendingEvent1, pendingEvent2);
    }

    @Test
    void shouldReturnEmptyListWhenNoPendingEvents() {
        OutboxEvent publishedEvent = createOutboxEvent("EVENT_1", 123L, "{\"data\":1}", OutboxEventStatus.PUBLISHED);
        when(repository.findAll()).thenReturn(Arrays.asList(publishedEvent));
        List<OutboxEvent> result = outboxEventServiceImpl.fetchPendingEvents(5);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldMarkAsPublished() {
        OutboxEvent event = createOutboxEvent("EVENT_1", 123L, "{\"data\":1}", OutboxEventStatus.PENDING);
        when(repository.save(any(OutboxEvent.class))).thenReturn(event);
        outboxEventServiceImpl.markAsPublished(event);
        verify(repository, times(1)).save(event);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getPublishedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void shouldMarkAsFailed() {
        OutboxEvent event = createOutboxEvent("EVENT_1", 123L, "{\"data\":1}", OutboxEventStatus.PENDING);
        when(repository.save(any(OutboxEvent.class))).thenReturn(event);
        outboxEventServiceImpl.markAsFailed(event);
        verify(repository, times(1)).save(event);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
    }

    private OutboxEvent createOutboxEvent(String eventType, Long aggregateId, String payload, OutboxEventStatus status) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setAggregateId(aggregateId);
        event.setPayload(payload);
        event.setStatus(status);
        return event;
    }
}
