package com.creditx.promo.service.impl;

import com.creditx.promo.dto.TransactionPostedEvent;
import com.creditx.promo.model.Promotion;
import com.creditx.promo.service.CashbackCalculatorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class CashbackCalculatorServiceImpl implements CashbackCalculatorService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public BigDecimal calculate(TransactionPostedEvent event, Promotion promotion) {
    try {
      JsonNode root = objectMapper.readTree(promotion.getRewardFormula());
      BigDecimal pct = root.path("cashbackPercent").decimalValue();
      BigDecimal max = root.path("maxCashback").decimalValue();
      BigDecimal cashback = event.getAmount().multiply(pct).divide(new BigDecimal("100"));
      if (cashback.compareTo(max) > 0) {
        cashback = max;
      }
      if (cashback.compareTo(BigDecimal.ZERO) < 0) {
        cashback = BigDecimal.ZERO;
      }
      return cashback;
    } catch (Exception e) {
      return BigDecimal.ZERO;
    }
  }
}
