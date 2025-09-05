package com.creditx.promo.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EventIdGeneratorTest {

    @Test
    void shouldGenerateUniqueEventId() {
        String eventType = "transaction.posted";
        Long transactionId = 123L;
        String eventId1 = EventIdGenerator.generateEventId(eventType, transactionId);
        String eventId2 = EventIdGenerator.generateEventId(eventType, transactionId);
        assertThat(eventId1).isNotNull();
        assertThat(eventId2).isNotNull();
        assertThat(eventId1).isNotEqualTo(eventId2);
        assertThat(eventId1).startsWith("transaction.posted-123-");
        assertThat(eventId2).startsWith("transaction.posted-123-");
        assertThat(eventId1).hasSize(eventType.length() + 1 + transactionId.toString().length() + 1 + 8);
    }

    @Test
    void shouldGenerateEventIdWithDifferentEventTypes() {
        Long transactionId = 456L;
        String eventId1 = EventIdGenerator.generateEventId("transaction.posted", transactionId);
        String eventId2 = EventIdGenerator.generateEventId("transaction.failed", transactionId);
        assertThat(eventId1).startsWith("transaction.posted-456-");
        assertThat(eventId2).startsWith("transaction.failed-456-");
        assertThat(eventId1).isNotEqualTo(eventId2);
    }

    @Test
    void shouldGenerateEventIdWithDifferentTransactionIds() {
        String eventType = "hold.created";
        String eventId1 = EventIdGenerator.generateEventId(eventType, 111L);
        String eventId2 = EventIdGenerator.generateEventId(eventType, 222L);
        assertThat(eventId1).startsWith("hold.created-111-");
        assertThat(eventId2).startsWith("hold.created-222-");
        assertThat(eventId1).isNotEqualTo(eventId2);
    }

    @Test
    void shouldGenerateConsistentPayloadHash() {
        String payload = "{\"transactionId\":123,\"promoId\":456}";
        String hash1 = EventIdGenerator.generatePayloadHash(payload);
        String hash2 = EventIdGenerator.generatePayloadHash(payload);
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64);
    }

    @Test
    void shouldGenerateDifferentHashesForDifferentPayloads() {
        String payload1 = "{\"transactionId\":123,\"promoId\":456}";
        String payload2 = "{\"transactionId\":789,\"promoId\":101}";
        String hash1 = EventIdGenerator.generatePayloadHash(payload1);
        String hash2 = EventIdGenerator.generatePayloadHash(payload2);
        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(hash1).hasSize(64);
        assertThat(hash2).hasSize(64);
    }

    @Test
    void shouldHandleEmptyPayload() {
        String hash = EventIdGenerator.generatePayloadHash("");
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64);
    }

    @Test
    void shouldThrowExceptionForNullPayload() {
        assertThrows(NullPointerException.class, () -> EventIdGenerator.generatePayloadHash(null));
    }

    @Test
    void shouldGenerateValidHexHash() {
        String payload = "{\"test\":\"data\"}";
        String hash = EventIdGenerator.generatePayloadHash(payload);
        assertThat(hash).matches("^[a-f0-9]{64}$");
    }
}
