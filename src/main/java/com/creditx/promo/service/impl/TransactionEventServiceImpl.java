package com.creditx.promo.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.creditx.promo.dto.TransactionPostedEvent;
import com.creditx.promo.service.ProcessedEventService;
import com.creditx.promo.service.TransactionEventService;
import com.creditx.promo.util.EventIdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import com.creditx.promo.tracing.TransactionSpanTagger;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventServiceImpl implements TransactionEventService {

    private final RestTemplate restTemplate;
    private final ProcessedEventService processedEventService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TransactionSpanTagger transactionSpanTagger;

    @Value("${app.creditmain.url:http://localhost:8080}")
    private String creditMainServiceUrl;

    @Override
    @Transactional
    public void processTransactionPosted(TransactionPostedEvent event) {
        // TODO: Implement Transaction Posted handler
    }
}
