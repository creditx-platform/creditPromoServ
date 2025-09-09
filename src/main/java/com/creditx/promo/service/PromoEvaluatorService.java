package com.creditx.promo.service;

import com.creditx.promo.dto.TransactionPostedEvent;
import com.creditx.promo.model.Promotion;
import java.util.List;

public interface PromoEvaluatorService {

  List<Promotion> evaluate(TransactionPostedEvent event, List<Promotion> activePromotions);
}
