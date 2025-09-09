package com.creditx.promo.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OutboxStreamPublisherTest {

  @Mock
  private StreamBridge streamBridge;

  @InjectMocks
  private OutboxStreamPublisher outboxStreamPublisher;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(outboxStreamPublisher, "bindingName", "test-binding");
  }

  @Test
  void shouldPublishWithKeyAndPayload() {
    String key = "test-key";
    String payload = "{\"name\":\"test-payload\", \"value\":100}";
    String eventType = "transaction.posted";
    @SuppressWarnings("unchecked") ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(
        Message.class);
    outboxStreamPublisher.publish(key, payload, eventType);
    verify(streamBridge, times(1)).send(org.mockito.ArgumentMatchers.eq("test-binding"),
        messageCaptor.capture());
    Message<String> sentMessage = messageCaptor.getValue();
    assertThat(sentMessage.getPayload()).isEqualTo(payload);
    assertThat(sentMessage.getHeaders().get("key")).isEqualTo(key);
    assertThat(sentMessage.getHeaders().get("eventType")).isEqualTo(eventType);
  }

  @Test
  void shouldNotPublishWithoutKey() {
    outboxStreamPublisher.publish(null, "{\"name\":\"test\"}", "transaction.posted");
    verify(streamBridge, never()).send(anyString(), anyString());
  }

  @Test
  void shouldNotPublishWithoutPayload() {
    outboxStreamPublisher.publish("test-key", "", "transaction.posted");
    verify(streamBridge, never()).send(anyString(), anyString());
  }

  @Test
  void shouldNotPublishWithoutEventType() {
    outboxStreamPublisher.publish("test-key", "{\"name\":\"test\"}", "");
    verify(streamBridge, never()).send(anyString(), anyString());
  }
}
