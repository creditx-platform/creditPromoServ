package com.creditx.promo.scheduler;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.creditx.promo.messaging.OutboxStreamPublisher;
import com.creditx.promo.model.OutboxEvent;
import com.creditx.promo.service.OutboxEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublishingScheduler {
    private final OutboxEventService outboxEventService;
    private final OutboxStreamPublisher outboxStreamPublisher;

    @Value("${app.outbox.batch-size}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval}")
    public void publishPendingEvents() {
        log.debug("Starting outbox event publishing cycle");
        List<OutboxEvent> events = outboxEventService.fetchPendingEvents(batchSize);
        
        if (events.isEmpty()) {
            log.debug("No pending outbox events to publish");
            return;
        }
        
        log.info("Publishing {} pending outbox events", events.size());
        int successCount = 0;
        int failureCount = 0;

        for (OutboxEvent event : events) {
            try {
                log.debug("Publishing event {} of type {}", event.getEventId(), event.getEventType());
                outboxStreamPublisher.publish(event.getAggregateId().toString(), event.getPayload(), event.getEventType());
                outboxEventService.markAsPublished(event);
                successCount++;
                log.debug("Successfully published event {}", event.getEventId());
            } catch (Exception e) {
                log.error("Failed to publish event {}: {}", event.getEventId(), e.getMessage(), e);
                outboxEventService.markAsFailed(event);
                failureCount++;
            }
        }
        
        log.info("Outbox publishing completed: {} successful, {} failed", successCount, failureCount);
    }
}
