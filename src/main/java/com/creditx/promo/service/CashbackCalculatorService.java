package com.creditx.promo.service;

import java.math.BigDecimal;
import com.creditx.promo.dto.TransactionPostedEvent;
import com.creditx.promo.model.Promotion;

public interface CashbackCalculatorService {
    BigDecimal calculate(TransactionPostedEvent event, Promotion promotion);
}
