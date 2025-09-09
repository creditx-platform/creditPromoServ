package com.creditx.promo.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creditx.promo.constants.EventTypes;
import com.creditx.promo.dto.TransactionPostedEvent;
import com.creditx.promo.service.TransactionEventService;
import com.creditx.promo.tracing.TransactionSpanTagger;
import com.creditx.promo.util.EventValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class TransactionEventListenerTest {

  @Mock
  private TransactionEventService transactionEventService;

  @Mock
  private TransactionSpanTagger transactionSpanTagger;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private TransactionEventListener transactionEventListener;

  private Consumer<Message<String>> transactionPostedConsumer;

  @BeforeEach
  void setup() {
    transactionPostedConsumer = transactionEventListener.transactionPosted();
  }

  @Test
  void shouldProcessValidTransactionPostedEvent() throws Exception {
    // given
    String payload = "{\"transactionId\":1,\"type\":\"INBOUND\",\"issuerAccountId\":10,\"merchantAccountId\":20,\"amount\":100,\"currency\":\"USD\",\"createdAt\":\"2025-06-01T00:00:00Z\"}";

    Message<String> message = MessageBuilder.withPayload(payload)
        .setHeader(EventTypes.EVENT_TYPE_HEADER, EventTypes.TRANSACTION_POSTED).build();

    TransactionPostedEvent event = TransactionPostedEvent.builder().transactionId(1L)
        .type(com.creditx.promo.model.TransactionType.INBOUND).issuerAccountId(10L)
        .merchantAccountId(20L).amount(new java.math.BigDecimal("100")).currency("USD")
        .createdAt(java.time.Instant.parse("2025-06-01T00:00:00Z")).build();

    try (MockedStatic<EventValidationUtils> mockedUtils = Mockito.mockStatic(
        EventValidationUtils.class)) {
      // given
      mockedUtils.when(
              () -> EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_POSTED))
          .thenReturn(true);
      when(objectMapper.readValue(payload, TransactionPostedEvent.class)).thenReturn(event);

      // when
      transactionPostedConsumer.accept(message);

      // then
      verify(transactionEventService, times(1)).processTransactionPosted(event);
      verify(transactionSpanTagger, times(1)).tagTransactionId(1L);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {EventTypes.HOLD_CREATED, EventTypes.HOLD_EXPIRED, EventTypes.HOLD_VOIDED,
      EventTypes.TRANSACTION_FAILED, EventTypes.TRANSACTION_INITIATED,
      EventTypes.TRANSACTION_AUTHORIZED})
  void shouldNotProcessInvalidTransactionPostedEvent(String eventType) throws Exception {
    // given
    String payload = "{\"transactionId\":1,\"type\":\"INBOUND\"}";

    Message<String> message = MessageBuilder.withPayload(payload)
        .setHeader(EventTypes.EVENT_TYPE_HEADER, eventType).build();

    try (MockedStatic<EventValidationUtils> mockedUtils = Mockito.mockStatic(
        EventValidationUtils.class)) {
      // given
      mockedUtils.when(
              () -> EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_POSTED))
          .thenReturn(false);

      // when
      transactionPostedConsumer.accept(message);

      // then
      verify(transactionEventService, never()).processTransactionPosted(
          any(TransactionPostedEvent.class));
      verify(transactionSpanTagger, never()).tagTransactionId(any(Long.class));
    }
  }

  @Test
  void shouldNotProcessWhenJsonDeserializationFails() throws Exception {
    // given
    String payload = "{invalid-json}";

    Message<String> message = MessageBuilder.withPayload(payload)
        .setHeader(EventTypes.EVENT_TYPE_HEADER, EventTypes.TRANSACTION_POSTED).build();

    try (MockedStatic<EventValidationUtils> mockedUtils = Mockito.mockStatic(
        EventValidationUtils.class)) {
      // given
      mockedUtils.when(
              () -> EventValidationUtils.validateEventType(message, EventTypes.TRANSACTION_POSTED))
          .thenReturn(true);
      when(objectMapper.readValue(payload, TransactionPostedEvent.class)).thenThrow(
          new com.fasterxml.jackson.core.JsonProcessingException("Invalid JSON") {
          });

      // when
      transactionPostedConsumer.accept(message);

      // then
      verify(transactionEventService, never()).processTransactionPosted(
          any(TransactionPostedEvent.class));
      verify(transactionSpanTagger, never()).tagTransactionId(any(Long.class));
    }
  }
}
