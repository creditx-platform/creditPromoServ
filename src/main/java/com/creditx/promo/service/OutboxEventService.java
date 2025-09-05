package com.creditx.promo.service;

import java.util.List;

import com.creditx.promo.model.OutboxEvent;

public interface OutboxEventService {
    OutboxEvent saveEvent(String eventType, Long aggregateId, String payload);

    List<OutboxEvent> fetchPendingEvents(int limit);

    void markAsPublished(OutboxEvent event);

    void markAsFailed(OutboxEvent event);
}