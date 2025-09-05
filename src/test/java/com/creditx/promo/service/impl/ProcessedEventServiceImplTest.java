package com.creditx.promo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.creditx.promo.model.ProcessedEvent;
import com.creditx.promo.repository.ProcessedEventRepository;

@ExtendWith(MockitoExtension.class)
class ProcessedEventServiceImplTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private ProcessedEventServiceImpl processedEventServiceImpl;

    @Test
    void shouldReturnTrueWhenEventIsProcessed() {
        String eventId = "event-123";
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(true);
        boolean result = processedEventServiceImpl.isEventProcessed(eventId);
        assertThat(result).isTrue();
        verify(processedEventRepository, times(1)).existsByEventId(eventId);
    }

    @Test
    void shouldReturnFalseWhenEventIsNotProcessed() {
        String eventId = "event-456";
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        boolean result = processedEventServiceImpl.isEventProcessed(eventId);
        assertThat(result).isFalse();
        verify(processedEventRepository, times(1)).existsByEventId(eventId);
    }

    @Test
    void shouldMarkEventAsProcessed() {
        String eventId = "event-789";
        String payloadHash = "hash-abc123";
        String status = "SUCCESS";
        ProcessedEvent savedEvent = createProcessedEvent(eventId, payloadHash, status);
        when(processedEventRepository.save(any(ProcessedEvent.class))).thenReturn(savedEvent);
        processedEventServiceImpl.markEventAsProcessed(eventId, payloadHash, status);
        ArgumentCaptor<ProcessedEvent> eventCaptor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository, times(1)).save(eventCaptor.capture());
        ProcessedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventId()).isEqualTo(eventId);
        assertThat(capturedEvent.getPayloadHash()).isEqualTo(payloadHash);
        assertThat(capturedEvent.getStatus()).isEqualTo(status);
    }

    @Test
    void shouldReturnTrueWhenPayloadIsProcessed() {
        String payloadHash = "hash-def456";
        when(processedEventRepository.existsByPayloadHash(payloadHash)).thenReturn(true);
        boolean result = processedEventServiceImpl.isPayloadProcessed(payloadHash);
        assertThat(result).isTrue();
        verify(processedEventRepository, times(1)).existsByPayloadHash(payloadHash);
    }

    @Test
    void shouldReturnFalseWhenPayloadIsNotProcessed() {
        String payloadHash = "hash-ghi789";
        when(processedEventRepository.existsByPayloadHash(payloadHash)).thenReturn(false);
        boolean result = processedEventServiceImpl.isPayloadProcessed(payloadHash);
        assertThat(result).isFalse();
        verify(processedEventRepository, times(1)).existsByPayloadHash(payloadHash);
    }

    private ProcessedEvent createProcessedEvent(String eventId, String payloadHash, String status) {
        return ProcessedEvent.builder()
                .eventId(eventId)
                .payloadHash(payloadHash)
                .status(status)
                .build();
    }
}
