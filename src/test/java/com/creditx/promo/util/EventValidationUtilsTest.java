package com.creditx.promo.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import com.creditx.promo.constants.EventTypes;

class EventValidationUtilsTest {

    @Test
    void shouldValidateCorrectEventType() {
        String payload = "{\"transactionId\":123}";
        Message<String> message = MessageBuilder
                .withPayload(payload)
                .setHeader(EventTypes.EVENT_TYPE_HEADER, EventTypes.TRANSACTION_POSTED)
                .build();
        boolean result = EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_POSTED);
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotValidateIncorrectEventType() {
        String payload = "{\"transactionId\":123}";
        Message<String> message = MessageBuilder
                .withPayload(payload)
                .setHeader(EventTypes.EVENT_TYPE_HEADER, EventTypes.HOLD_CREATED)
                .build();
        boolean result = EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_POSTED);
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotValidateWhenEventTypeHeaderMissing() {
        String payload = "{\"transactionId\":123}";
        Message<String> message = MessageBuilder
                .withPayload(payload)
                .build();
        boolean result = EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_POSTED);
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotValidateNullMessage() {
        Message<String> nullMessage = null;
        boolean result = EventValidationUtils.validateEventType(nullMessage, EventTypes.TRANSACTION_POSTED);
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotValidateNullExpectedEventType() {
        String payload = "{\"transactionId\":123}";
        Message<String> message = MessageBuilder
                .withPayload(payload)
                .setHeader(EventTypes.EVENT_TYPE_HEADER, EventTypes.TRANSACTION_POSTED)
                .build();
        boolean result = EventValidationUtils.validateEventType(message, null);
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotValidateEmptyExpectedEventType() {
        String payload = "{\"transactionId\":123}";
        Message<String> message = MessageBuilder
                .withPayload(payload)
                .setHeader(EventTypes.EVENT_TYPE_HEADER, EventTypes.TRANSACTION_POSTED)
                .build();
        boolean result = EventValidationUtils.validateEventType(message, "");
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotValidateWhitespaceExpectedEventType() {
        String payload = "{\"transactionId\":123}";
        Message<String> message = MessageBuilder
                .withPayload(payload)
                .setHeader(EventTypes.EVENT_TYPE_HEADER, EventTypes.TRANSACTION_POSTED)
                .build();
        boolean result = EventValidationUtils.validateEventType(message, "   ");
        assertThat(result).isFalse();
    }

    @Test
    void shouldValidateWithCustomEventTypeHeader() {
        String payload = "{\"promoId\":456}";
        Map<String, Object> headers = new HashMap<>();
        headers.put(EventTypes.EVENT_TYPE_HEADER, "custom.event.type");
        Message<String> message = MessageBuilder
                .withPayload(payload)
                .copyHeaders(headers)
                .build();
        boolean result = EventValidationUtils.validateEventType(message, "custom.event.type");
        assertThat(result).isTrue();
    }

    @Test
    void shouldGetEventTypeFromMessage() {
        String payload = "{\"transactionId\":123}";
        Message<String> message = MessageBuilder
                .withPayload(payload)
                .setHeader(EventTypes.EVENT_TYPE_HEADER, EventTypes.TRANSACTION_POSTED)
                .build();
        String eventType = EventValidationUtils.getEventType(message);
        assertThat(eventType).isEqualTo(EventTypes.TRANSACTION_POSTED);
    }

    @Test
    void shouldReturnNullWhenEventTypeHeaderMissing() {
        String payload = "{\"transactionId\":123}";
        Message<String> message = MessageBuilder
                .withPayload(payload)
                .build();
        String eventType = EventValidationUtils.getEventType(message);
        assertThat(eventType).isNull();
    }

    @Test
    void shouldReturnNullForNullMessage() {
        Message<String> nullMessage = null;
        String eventType = EventValidationUtils.getEventType(nullMessage);
        assertThat(eventType).isNull();
    }

    @Test
    void shouldHandleNonStringEventTypeHeader() {
        String payload = "{\"transactionId\":123}";
        Map<String, Object> headers = new HashMap<>();
        headers.put(EventTypes.EVENT_TYPE_HEADER, 12345);
        Message<String> message = MessageBuilder
                .withPayload(payload)
                .copyHeaders(headers)
                .build();
        String eventType = EventValidationUtils.getEventType(message);
        boolean isValid = EventValidationUtils.validateEventType(message, "12345");
        assertThat(eventType).isEqualTo("12345");
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldValidateAllEventTypes() {
        String[] allEventTypes = {
            EventTypes.TRANSACTION_AUTHORIZED,
            EventTypes.TRANSACTION_INITIATED,
            EventTypes.TRANSACTION_POSTED,
            EventTypes.TRANSACTION_FAILED,
            EventTypes.HOLD_CREATED,
            EventTypes.HOLD_EXPIRED,
            EventTypes.HOLD_VOIDED
        };
        for (String eventType : allEventTypes) {
            String payload = "{\"test\":\"data\"}";
            Message<String> message = MessageBuilder
                    .withPayload(payload)
                    .setHeader(EventTypes.EVENT_TYPE_HEADER, eventType)
                    .build();
            boolean result = EventValidationUtils.validateEventType(message, eventType);
            assertThat(result).as("Event type %s should be valid", eventType).isTrue();
        }
    }
}
