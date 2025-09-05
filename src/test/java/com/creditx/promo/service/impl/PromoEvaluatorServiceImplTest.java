package com.creditx.promo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.creditx.promo.dto.TransactionPostedEvent;
import com.creditx.promo.model.Promotion;
import com.creditx.promo.model.PromotionStatus;
import com.creditx.promo.model.TransactionType;

@ExtendWith(MockitoExtension.class)
class PromoEvaluatorServiceImplTest {

    @InjectMocks
    private PromoEvaluatorServiceImpl promoEvaluatorService;

    private TransactionPostedEvent testEvent;
    private Promotion testPromotion;

    @BeforeEach
    void setup() {
        testEvent = TransactionPostedEvent.builder()
            .transactionId(10L)
            .issuerAccountId(1L)
            .merchantAccountId(200L)
            .amount(new BigDecimal("100"))
            .currency("USD")
            .type(TransactionType.INBOUND)
            .createdAt(Instant.parse("2025-06-01T00:00:00Z"))
            .build();

        testPromotion = Promotion.builder()
            .promoId("P1")
            .name("Test Promo")
            .startDate(Instant.parse("2025-01-01T00:00:00Z"))
            .expiryDate(Instant.parse("2025-12-31T00:00:00Z"))
            .eligibilityRules("{\"minAmount\":50,\"merchantIds\":[200]}")
            .rewardFormula("{\"cashbackPercent\":10,\"maxCashback\":25}")
            .status(PromotionStatus.ACTIVE)
            .build();
    }

    @Test
    void shouldEvaluatePromotionEligibleByAmountAndMerchant() {
        // given
        List<Promotion> promotions = List.of(testPromotion);

        // when
        List<Promotion> result = promoEvaluatorService.evaluate(testEvent, promotions);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPromoId()).isEqualTo("P1");
    }

    @Test
    void shouldFilterOutInactivePromotions() {
        // given
        Promotion inactivePromotion = Promotion.builder()
            .promoId("INACTIVE")
            .name("Inactive Promo")
            .startDate(Instant.parse("2025-01-01T00:00:00Z"))
            .expiryDate(Instant.parse("2025-12-31T00:00:00Z"))
            .eligibilityRules("{\"minAmount\":50}")
            .rewardFormula("{\"cashbackPercent\":5,\"maxCashback\":10}")
            .status(PromotionStatus.INACTIVE)
            .build();

        List<Promotion> promotions = List.of(inactivePromotion, testPromotion);

        // when
        List<Promotion> result = promoEvaluatorService.evaluate(testEvent, promotions);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPromoId()).isEqualTo("P1");
    }

    @Test
    void shouldFilterOutPromotionsWithAmountTooLow() {
        // given
        TransactionPostedEvent lowAmountEvent = TransactionPostedEvent.builder()
            .transactionId(11L)
            .issuerAccountId(1L)
            .merchantAccountId(200L)
            .amount(new BigDecimal("10"))
            .currency("USD")
            .type(TransactionType.INBOUND)
            .createdAt(Instant.parse("2025-06-01T00:00:00Z"))
            .build();

        List<Promotion> promotions = List.of(testPromotion);

        // when
        List<Promotion> result = promoEvaluatorService.evaluate(lowAmountEvent, promotions);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldFilterOutPromotionsWithWrongMerchant() {
        // given
        TransactionPostedEvent wrongMerchantEvent = TransactionPostedEvent.builder()
            .transactionId(12L)
            .issuerAccountId(1L)
            .merchantAccountId(999L) // Different merchant
            .amount(new BigDecimal("100"))
            .currency("USD")
            .type(TransactionType.INBOUND)
            .createdAt(Instant.parse("2025-06-01T00:00:00Z"))
            .build();

        List<Promotion> promotions = List.of(testPromotion);

        // when
        List<Promotion> result = promoEvaluatorService.evaluate(wrongMerchantEvent, promotions);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldFilterOutExpiredPromotions() {
        // given
        TransactionPostedEvent futureEvent = TransactionPostedEvent.builder()
            .transactionId(13L)
            .issuerAccountId(1L)
            .merchantAccountId(200L)
            .amount(new BigDecimal("100"))
            .currency("USD")
            .type(TransactionType.INBOUND)
            .createdAt(Instant.parse("2026-06-01T00:00:00Z")) // After expiry
            .build();

        List<Promotion> promotions = List.of(testPromotion);

        // when
        List<Promotion> result = promoEvaluatorService.evaluate(futureEvent, promotions);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldAllowPromotionWithoutMerchantRestriction() {
        // given
        Promotion noMerchantPromo = Promotion.builder()
            .promoId("P2")
            .name("No Merchant Restriction")
            .startDate(Instant.parse("2025-01-01T00:00:00Z"))
            .expiryDate(Instant.parse("2025-12-31T00:00:00Z"))
            .eligibilityRules("{\"minAmount\":50}")
            .rewardFormula("{\"cashbackPercent\":5,\"maxCashback\":10}")
            .status(PromotionStatus.ACTIVE)
            .build();

        List<Promotion> promotions = List.of(noMerchantPromo);

        // when
        List<Promotion> result = promoEvaluatorService.evaluate(testEvent, promotions);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPromoId()).isEqualTo("P2");
    }

    @Test
    void shouldHandleInvalidJson() {
        // given
        Promotion invalidJsonPromo = Promotion.builder()
            .promoId("INVALID")
            .name("Invalid JSON Promo")
            .startDate(Instant.parse("2025-01-01T00:00:00Z"))
            .expiryDate(Instant.parse("2025-12-31T00:00:00Z"))
            .eligibilityRules("not-valid-json")
            .rewardFormula("{\"cashbackPercent\":5,\"maxCashback\":10}")
            .status(PromotionStatus.ACTIVE)
            .build();

        List<Promotion> promotions = List.of(invalidJsonPromo);

        // when
        List<Promotion> result = promoEvaluatorService.evaluate(testEvent, promotions);

        // then
        assertThat(result).isEmpty();
    }
}