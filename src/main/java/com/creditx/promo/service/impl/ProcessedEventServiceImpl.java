package com.creditx.promo.service.impl;

import org.springframework.stereotype.Service;

import com.creditx.promo.model.ProcessedEvent;
import com.creditx.promo.repository.ProcessedEventRepository;
import com.creditx.promo.service.ProcessedEventService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventServiceImpl implements ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    @Override
    public boolean isEventProcessed(String eventId) {
        boolean exists = processedEventRepository.existsByEventId(eventId);
        if (exists) {
            log.debug("Event {} has already been processed", eventId);
        }
        return exists;
    }

    @Override
    @Transactional
    public void markEventAsProcessed(String eventId, String payloadHash, String status) {
        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .payloadHash(payloadHash)
                .status(status)
                .build();
        
        processedEventRepository.save(processedEvent);
        log.debug("Marked event {} as processed with status {}", eventId, status);
    }

    @Override
    public boolean isPayloadProcessed(String payloadHash) {
        boolean exists = processedEventRepository.existsByPayloadHash(payloadHash);
        if (exists) {
            log.debug("Payload with hash {} has already been processed", payloadHash);
        }
        return exists;
    }
}
