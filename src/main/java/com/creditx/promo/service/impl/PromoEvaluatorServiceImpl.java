package com.creditx.promo.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.creditx.promo.dto.TransactionPostedEvent;
import com.creditx.promo.model.Promotion;
import com.creditx.promo.model.PromotionStatus;
import com.creditx.promo.service.PromoEvaluatorService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PromoEvaluatorServiceImpl implements PromoEvaluatorService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Promotion> evaluate(TransactionPostedEvent event, List<Promotion> activePromotions) {
    Instant txnTime = event.getCreatedAt();
    return activePromotions.stream()
        .filter(p -> p.getStatus() != PromotionStatus.INACTIVE && !txnTime.isBefore(p.getStartDate()) && txnTime.isBefore(p.getExpiryDate()))
                .filter(p -> matches(event, p))
                .collect(Collectors.toList());
    }

    private boolean matches(TransactionPostedEvent event, Promotion promo) {
        try {
            JsonNode root = objectMapper.readTree(promo.getEligibilityRules());
            if (root.has("minAmount") && event.getAmount().compareTo(root.get("minAmount").decimalValue()) < 0) return false;
            if (root.has("merchantIds")) {
                boolean ok = false;
                for (JsonNode n : root.get("merchantIds")) if (n.asLong() == event.getMerchantAccountId()) { ok = true; break; }
                if (!ok) return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Eligibility parse failed for promo {}: {}", promo.getPromoId(), e.getMessage());
            return false;
        }
    }
}
