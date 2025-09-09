package com.creditx.promo.service;

import com.creditx.promo.dto.TransactionPostedEvent;
import com.creditx.promo.model.Promotion;
import java.math.BigDecimal;

public interface CashbackCalculatorService {

  BigDecimal calculate(TransactionPostedEvent event, Promotion promotion);
}
