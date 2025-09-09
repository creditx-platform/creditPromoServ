package com.creditx.promo.service.impl;

import com.creditx.promo.dto.TransactionPostedEvent;
import com.creditx.promo.model.Promotion;
import com.creditx.promo.model.PromotionApplication;
import com.creditx.promo.model.PromotionApplicationStatus;
import com.creditx.promo.model.PromotionStatus;
import com.creditx.promo.repository.PromotionApplicationRepository;
import com.creditx.promo.repository.PromotionRepository;
import com.creditx.promo.service.CashbackCalculatorService;
import com.creditx.promo.service.ProcessedEventService;
import com.creditx.promo.service.PromoEvaluatorService;
import com.creditx.promo.service.TransactionEventService;
import com.creditx.promo.util.EventIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventServiceImpl implements TransactionEventService {

  private final RestTemplate restTemplate;
  private final ProcessedEventService processedEventService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PromoEvaluatorService promoEvaluatorService;
  private final CashbackCalculatorService cashbackCalculatorService;
  private final PromotionRepository promotionRepository;
  private final PromotionApplicationRepository promotionApplicationRepository;

  @Value("${app.creditmain.url:http://localhost:8080}")
  private String creditMainServiceUrl;

  @Override
  @Transactional
  public void processTransactionPosted(TransactionPostedEvent event) {
    String eventId = EventIdGenerator.generateEventId("transaction.posted",
        event.getTransactionId());
    if (processedEventService.isEventProcessed(eventId)) {
      log.debug("Event {} already processed", eventId);
      return;
    }
    // Branch on transaction type
    switch (event.getType()) {
      case INBOUND -> handleInbound(event, eventId);
      case CASHBACK -> handleCashbackPosted(event, eventId);
      default -> processedEventService.markEventAsProcessed(eventId, null, "IGNORED_TYPE");
    }
  }

  private void handleInbound(TransactionPostedEvent event, String eventId) {
    // Fetch promotions whose validity window includes the transaction createdAt (status ignored except INACTIVE)
    var promos = promotionRepository.findByStartDateLessThanEqualAndExpiryDateGreaterThanEqual(
            event.getCreatedAt(), event.getCreatedAt()).stream().filter(
            p -> p.getStatus() != PromotionStatus.INACTIVE) // allow ACTIVE or EXPIRED (retroactive)
        .toList();
    var matching = promoEvaluatorService.evaluate(event, promos);
    if (matching.isEmpty()) {
      processedEventService.markEventAsProcessed(eventId, null, "NO_PROMO");
      return;
    }
    Promotion promo = matching.get(0);
    String idempotencyKey = promo.getPromoId() + ":" + event.getTransactionId();
    if (promotionApplicationRepository.existsByIdempotencyKey(idempotencyKey)) {
      processedEventService.markEventAsProcessed(eventId, null, "DUPLICATE");
      return;
    }
    var cashback = cashbackCalculatorService.calculate(event, promo);
    if (cashback.compareTo(java.math.BigDecimal.ZERO) <= 0) {
      processedEventService.markEventAsProcessed(eventId, null, "NO_CASHBACK");
      return;
    }
    PromotionApplication application;
    try {
      // Build request matching CreateCashbackTransactionRequest in main service
      var request = new java.util.HashMap<String, Object>();
      // We invert issuer/merchant to credit original issuer (customer) and debit merchant
      request.put("issuerAccountId", event.getMerchantAccountId());
      request.put("merchantAccountId", event.getIssuerAccountId());
      request.put("amount", cashback);
      request.put("currency", event.getCurrency());
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      var entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);
      restTemplate.postForEntity(creditMainServiceUrl + "/api/transactions/cashback", entity,
          String.class);
      application = PromotionApplication.builder()
          .applicationId(java.util.UUID.randomUUID().toString()).promoId(promo.getPromoId())
          .transactionId(event.getTransactionId()).issuerId(event.getIssuerAccountId())
          .merchantId(event.getMerchantAccountId()).cashbackAmount(cashback)
          .status(PromotionApplicationStatus.APPLIED).reason(null).idempotencyKey(idempotencyKey)
          .build();
      promotionApplicationRepository.save(application);
      processedEventService.markEventAsProcessed(eventId, null, "APPLIED");
    } catch (Exception e) {
      log.error("Cashback creation failed for promo {} txn {}", promo.getPromoId(),
          event.getTransactionId(), e);
      application = PromotionApplication.builder()
          .applicationId(java.util.UUID.randomUUID().toString()).promoId(promo.getPromoId())
          .transactionId(event.getTransactionId()).issuerId(event.getIssuerAccountId())
          .merchantId(event.getMerchantAccountId()).cashbackAmount(cashback)
          .status(PromotionApplicationStatus.FAILED)
          .reason(e.getClass().getSimpleName() + ":" + e.getMessage())
          .idempotencyKey(idempotencyKey).build();
      promotionApplicationRepository.save(application);
      processedEventService.markEventAsProcessed(eventId, null, "FAILED");
    }
  }

  private void handleCashbackPosted(TransactionPostedEvent event, String eventId) {
    // Cashback transaction posted - nothing to evaluate; mark processed to stop loops
    processedEventService.markEventAsProcessed(eventId, null, "CASHBACK_CONFIRMED");
  }
}
