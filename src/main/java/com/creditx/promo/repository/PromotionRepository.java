package com.creditx.promo.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditx.promo.model.Promotion;
import com.creditx.promo.model.PromotionStatus;

public interface PromotionRepository extends JpaRepository<Promotion, String> {
    List<Promotion> findByStatusAndStartDateLessThanEqualAndExpiryDateGreaterThanEqual(PromotionStatus status, Instant start, Instant end);
    List<Promotion> findByStartDateLessThanEqualAndExpiryDateGreaterThanEqual(Instant start, Instant end);
}
