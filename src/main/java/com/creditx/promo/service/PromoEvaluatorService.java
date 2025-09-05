package com.creditx.promo.service;

import java.util.List;
import com.creditx.promo.dto.TransactionPostedEvent;
import com.creditx.promo.model.Promotion;

public interface PromoEvaluatorService {
    List<Promotion> evaluate(TransactionPostedEvent event, List<Promotion> activePromotions);
}
