package com.creditx.promo.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import com.creditx.promo.constants.EventTypes;
import com.creditx.promo.dto.TransactionPostedEvent;
import com.creditx.promo.service.TransactionEventService;
import com.creditx.promo.tracing.TransactionSpanTagger;
import com.creditx.promo.util.EventValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TransactionEventListener {

    private final TransactionEventService transactionEventService;
    private final TransactionSpanTagger transactionSpanTagger;
    private final ObjectMapper objectMapper;

    @Bean
    public Consumer<Message<String>> transactionPosted() {
        return message -> {
            String payload = message.getPayload();
            // TODO: Implemenet transaction posted handler
        };
    }
}
