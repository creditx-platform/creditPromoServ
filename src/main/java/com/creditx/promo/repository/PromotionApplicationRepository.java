package com.creditx.promo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditx.promo.model.PromotionApplication;

public interface PromotionApplicationRepository extends JpaRepository<PromotionApplication, String> {
    boolean existsByTransactionIdAndPromoId(Long transactionId, String promoId);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
