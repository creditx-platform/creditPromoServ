package com.creditx.promo.repository;

import com.creditx.promo.model.PromotionApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionApplicationRepository extends
    JpaRepository<PromotionApplication, String> {

  boolean existsByTransactionIdAndPromoId(Long transactionId, String promoId);

  boolean existsByIdempotencyKey(String idempotencyKey);
}
