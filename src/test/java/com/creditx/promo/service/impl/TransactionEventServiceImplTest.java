package com.creditx.promo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.creditx.promo.dto.TransactionPostedEvent;
import com.creditx.promo.model.Promotion;
import com.creditx.promo.model.PromotionApplication;
import com.creditx.promo.model.PromotionApplicationStatus;
import com.creditx.promo.model.PromotionStatus;
import com.creditx.promo.model.TransactionType;
import com.creditx.promo.repository.PromotionApplicationRepository;
import com.creditx.promo.repository.PromotionRepository;
import com.creditx.promo.service.CashbackCalculatorService;
import com.creditx.promo.service.ProcessedEventService;
import com.creditx.promo.service.PromoEvaluatorService;
import com.creditx.promo.util.EventIdGenerator;

@ExtendWith(MockitoExtension.class)
class TransactionEventServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ProcessedEventService processedEventService;

    @Mock
    private PromoEvaluatorService promoEvaluatorService;

    @Mock
    private CashbackCalculatorService cashbackCalculatorService;

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private PromotionApplicationRepository promotionApplicationRepository;

    @InjectMocks
    private TransactionEventServiceImpl transactionEventService;

    private TransactionPostedEvent baseEvent;
    private Promotion testPromotion;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(transactionEventService, "creditMainServiceUrl", "http://localhost:8080");
        
        baseEvent = TransactionPostedEvent.builder()
            .transactionId(100L)
            .issuerAccountId(10L)
            .merchantAccountId(20L)
            .amount(new BigDecimal("200"))
            .currency("USD")
            .type(TransactionType.INBOUND)
            .createdAt(Instant.parse("2025-06-01T00:00:00Z"))
            .build();

        testPromotion = Promotion.builder()
            .promoId("PROMO1")
            .name("Test Promo")
            .startDate(baseEvent.getCreatedAt().minusSeconds(10))
            .expiryDate(baseEvent.getCreatedAt().plusSeconds(10))
            .eligibilityRules("{\"minAmount\":50}")
            .rewardFormula("{\"cashbackPercent\":10,\"maxCashback\":999}")
            .status(PromotionStatus.ACTIVE)
            .build();
    }

    @Test
    void shouldSkipProcessingWhenEventAlreadyProcessed() {
        // given
        String eventId = "transaction.posted-100-12345678";

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.posted", 100L))
                    .thenReturn(eventId);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(true);

            // when
            transactionEventService.processTransactionPosted(baseEvent);

            // then
            verify(processedEventService, times(1)).isEventProcessed(eventId);
            verify(promoEvaluatorService, never()).evaluate(any(), any());
            verify(cashbackCalculatorService, never()).calculate(any(), any());
            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }
    }

    @Test
    void shouldApplyPromotionSuccessfully() {
        // given
        String eventId = "transaction.posted-100-12345678";
        String idempotencyKey = "PROMO1:100";
        BigDecimal cashbackAmount = new BigDecimal("20.00");

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.posted", 100L))
                    .thenReturn(eventId);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);
            when(promotionRepository.findByStartDateLessThanEqualAndExpiryDateGreaterThanEqual(any(), any()))
                    .thenReturn(List.of(testPromotion));
            when(promoEvaluatorService.evaluate(eq(baseEvent), anyList())).thenReturn(List.of(testPromotion));
            when(cashbackCalculatorService.calculate(eq(baseEvent), eq(testPromotion))).thenReturn(cashbackAmount);
            when(promotionApplicationRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok("Success"));

            // when
            transactionEventService.processTransactionPosted(baseEvent);

            // then
            verify(processedEventService, times(1)).isEventProcessed(eventId);
            verify(promotionRepository, times(1)).findByStartDateLessThanEqualAndExpiryDateGreaterThanEqual(
                    baseEvent.getCreatedAt(), baseEvent.getCreatedAt());
            verify(promoEvaluatorService, times(1)).evaluate(baseEvent, List.of(testPromotion));
            verify(cashbackCalculatorService, times(1)).calculate(baseEvent, testPromotion);
            verify(promotionApplicationRepository, times(1)).existsByIdempotencyKey(idempotencyKey);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate, times(1)).postForEntity(urlCaptor.capture(), entityCaptor.capture(), eq(String.class));

            ArgumentCaptor<PromotionApplication> applicationCaptor = ArgumentCaptor.forClass(PromotionApplication.class);
            verify(promotionApplicationRepository, times(1)).save(applicationCaptor.capture());
            
            PromotionApplication savedApplication = applicationCaptor.getValue();
            assertThat(savedApplication.getPromoId()).isEqualTo("PROMO1");
            assertThat(savedApplication.getTransactionId()).isEqualTo(100L);
            assertThat(savedApplication.getCashbackAmount()).isEqualByComparingTo(cashbackAmount);
            assertThat(savedApplication.getStatus()).isEqualTo(PromotionApplicationStatus.APPLIED);
            assertThat(savedApplication.getIdempotencyKey()).isEqualTo(idempotencyKey);

            verify(processedEventService, times(1)).markEventAsProcessed(eventId, null, "APPLIED");
        }
    }

    @Test
    void shouldSkipWhenNoPromotionMatches() {
        // given
        String eventId = "transaction.posted-100-12345678";

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.posted", 100L))
                    .thenReturn(eventId);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);
            when(promotionRepository.findByStartDateLessThanEqualAndExpiryDateGreaterThanEqual(any(), any()))
                    .thenReturn(List.of(testPromotion));
            when(promoEvaluatorService.evaluate(eq(baseEvent), anyList())).thenReturn(List.of()); // No matches after evaluation

            // when
            transactionEventService.processTransactionPosted(baseEvent);

            // then
            verify(processedEventService, times(1)).markEventAsProcessed(eventId, null, "NO_PROMO");
            verify(promoEvaluatorService, times(1)).evaluate(baseEvent, List.of(testPromotion));
            verify(cashbackCalculatorService, never()).calculate(any(), any());
            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }
    }

    @Test
    void shouldSkipWhenDuplicateApplication() {
        // given
        String eventId = "transaction.posted-100-12345678";
        String idempotencyKey = "PROMO1:100";

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.posted", 100L))
                    .thenReturn(eventId);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);
            when(promotionRepository.findByStartDateLessThanEqualAndExpiryDateGreaterThanEqual(any(), any()))
                    .thenReturn(List.of(testPromotion));
            when(promoEvaluatorService.evaluate(eq(baseEvent), anyList())).thenReturn(List.of(testPromotion));
            when(promotionApplicationRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(true);

            // when
            transactionEventService.processTransactionPosted(baseEvent);

            // then
            verify(processedEventService, times(1)).markEventAsProcessed(eventId, null, "DUPLICATE");
            verify(cashbackCalculatorService, never()).calculate(any(), any());
            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }
    }

    @Test
    void shouldMarkAsFailedWhenCashbackCallFails() {
        // given
        String eventId = "transaction.posted-100-12345678";
        String idempotencyKey = "PROMO1:100";
        BigDecimal cashbackAmount = new BigDecimal("20.00");

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.posted", 100L))
                    .thenReturn(eventId);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);
            when(promotionRepository.findByStartDateLessThanEqualAndExpiryDateGreaterThanEqual(any(), any()))
                    .thenReturn(List.of(testPromotion));
            when(promoEvaluatorService.evaluate(eq(baseEvent), anyList())).thenReturn(List.of(testPromotion));
            when(cashbackCalculatorService.calculate(eq(baseEvent), eq(testPromotion))).thenReturn(cashbackAmount);
            when(promotionApplicationRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new RuntimeException("Service unavailable"));

            // when
            transactionEventService.processTransactionPosted(baseEvent);

            // then
            ArgumentCaptor<PromotionApplication> applicationCaptor = ArgumentCaptor.forClass(PromotionApplication.class);
            verify(promotionApplicationRepository, times(1)).save(applicationCaptor.capture());
            
            PromotionApplication savedApplication = applicationCaptor.getValue();
            assertThat(savedApplication.getStatus()).isEqualTo(PromotionApplicationStatus.FAILED);
            assertThat(savedApplication.getReason()).contains("RuntimeException:Service unavailable");

            verify(processedEventService, times(1)).markEventAsProcessed(eventId, null, "FAILED");
        }
    }

    @Test
    void shouldIgnoreCashbackTransactionType() {
        // given
        TransactionPostedEvent cashbackEvent = TransactionPostedEvent.builder()
            .transactionId(200L)
            .type(TransactionType.CASHBACK)
            .issuerAccountId(10L)
            .merchantAccountId(20L)
            .amount(new BigDecimal("5.00"))
            .currency("USD")
            .createdAt(Instant.now())
            .build();

        String eventId = "transaction.posted-200-12345678";

        try (MockedStatic<EventIdGenerator> mockedGenerator = Mockito.mockStatic(EventIdGenerator.class)) {
            mockedGenerator.when(() -> EventIdGenerator.generateEventId("transaction.posted", 200L))
                    .thenReturn(eventId);

            when(processedEventService.isEventProcessed(eventId)).thenReturn(false);

            // when
            transactionEventService.processTransactionPosted(cashbackEvent);

            // then
            verify(processedEventService, times(1)).markEventAsProcessed(eventId, null, "CASHBACK_CONFIRMED");
            verify(promotionRepository, never()).findByStartDateLessThanEqualAndExpiryDateGreaterThanEqual(any(), any());
            verify(promoEvaluatorService, never()).evaluate(any(), any());
        }
    }
}