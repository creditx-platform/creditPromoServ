package com.creditx.promo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

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
class CashbackCalculatorServiceImplTest {

    @InjectMocks
    private CashbackCalculatorServiceImpl cashbackCalculatorService;

    private TransactionPostedEvent testEvent;
    private Promotion testPromotion;

    @BeforeEach
    void setup() {
        testEvent = TransactionPostedEvent.builder()
            .transactionId(1L)
            .issuerAccountId(1L)
            .merchantAccountId(2L)
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .type(TransactionType.INBOUND)
            .createdAt(Instant.now())
            .build();

        testPromotion = Promotion.builder()
            .promoId("P1")
            .name("Test Promo")
            .startDate(Instant.now())
            .expiryDate(Instant.now().plusSeconds(3600))
            .eligibilityRules("{}")
            .rewardFormula("{\"cashbackPercent\":10,\"maxCashback\":5}")
            .status(PromotionStatus.ACTIVE)
            .build();
    }

    @Test
    void shouldCalculatePercentWithCap() {
        // given
        TransactionPostedEvent event = TransactionPostedEvent.builder()
            .transactionId(1L)
            .issuerAccountId(1L)
            .merchantAccountId(2L)
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .type(TransactionType.INBOUND)
            .createdAt(Instant.now())
            .build();
        
        // when
        BigDecimal result = cashbackCalculatorService.calculate(event, testPromotion);
        
        // then
        assertThat(result).isEqualByComparingTo("5.00"); // 10% of 100 = 10, but capped at 5
    }

    @Test
    void shouldCalculatePercentWithoutCap() {
        // given
        TransactionPostedEvent event = TransactionPostedEvent.builder()
            .transactionId(1L)
            .issuerAccountId(1L)
            .merchantAccountId(2L)
            .amount(new BigDecimal("20.00"))
            .currency("USD")
            .type(TransactionType.INBOUND)
            .createdAt(Instant.now())
            .build();
        
        // when
        BigDecimal result = cashbackCalculatorService.calculate(event, testPromotion);
        
        // then
        assertThat(result).isEqualByComparingTo("2.00"); // 10% of 20 = 2, under cap of 5
    }

    @Test
    void shouldReturnZeroForInvalidJson() {
        // given
        Promotion badPromotion = Promotion.builder()
            .promoId("P2")
            .name("Bad Promo")
            .startDate(Instant.now())
            .expiryDate(Instant.now().plusSeconds(3600))
            .eligibilityRules("{}")
            .rewardFormula("not-json")
            .status(PromotionStatus.ACTIVE)
            .build();
        
        // when
        BigDecimal result = cashbackCalculatorService.calculate(testEvent, badPromotion);
        
        // then
        assertThat(result).isZero();
    }

    @Test
    void shouldReturnZeroForMissingFields() {
        // given
        Promotion incompletePromotion = Promotion.builder()
            .promoId("P3")
            .name("Incomplete Promo")
            .startDate(Instant.now())
            .expiryDate(Instant.now().plusSeconds(3600))
            .eligibilityRules("{}")
            .rewardFormula("{\"cashbackPercent\":10}")
            .status(PromotionStatus.ACTIVE)
            .build();
        
        // when
        BigDecimal result = cashbackCalculatorService.calculate(testEvent, incompletePromotion);
        
        // then
        assertThat(result).isZero();
    }

    @Test
    void shouldHandleZeroAmount() {
        // given
        TransactionPostedEvent zeroEvent = TransactionPostedEvent.builder()
            .transactionId(1L)
            .issuerAccountId(1L)
            .merchantAccountId(2L)
            .amount(BigDecimal.ZERO)
            .currency("USD")
            .type(TransactionType.INBOUND)
            .createdAt(Instant.now())
            .build();
        
        // when
        BigDecimal result = cashbackCalculatorService.calculate(zeroEvent, testPromotion);
        
        // then
        assertThat(result).isZero();
    }

    @Test
    void shouldHandleHighPercentageWithCap() {
        // given
        Promotion highPercentPromotion = Promotion.builder()
            .promoId("P4")
            .name("High Percent Promo")
            .startDate(Instant.now())
            .expiryDate(Instant.now().plusSeconds(3600))
            .eligibilityRules("{}")
            .rewardFormula("{\"cashbackPercent\":50,\"maxCashback\":10}")
            .status(PromotionStatus.ACTIVE)
            .build();
        
        TransactionPostedEvent largeEvent = TransactionPostedEvent.builder()
            .transactionId(1L)
            .issuerAccountId(1L)
            .merchantAccountId(2L)
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .type(TransactionType.INBOUND)
            .createdAt(Instant.now())
            .build();
        
        // when
        BigDecimal result = cashbackCalculatorService.calculate(largeEvent, highPercentPromotion);
        
        // then
        assertThat(result).isEqualByComparingTo("10.00"); // 50% of 100 = 50, but capped at 10
    }
}