package com.creditx.promo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CPRS_PROMO_APPLICATIONS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionApplication {

  @Id
  @Column(name = "APPLICATION_ID", length = 36)
  private String applicationId;

  @Column(name = "PROMO_ID", nullable = false, length = 36)
  private String promoId;

  @Column(name = "TRANSACTION_ID", nullable = false)
  private Long transactionId;

  @Column(name = "ISSUER_ID", nullable = false)
  private Long issuerId;

  @Column(name = "MERCHANT_ID")
  private Long merchantId;

  @Column(name = "CASHBACK_AMOUNT", nullable = false, precision = 20, scale = 2)
  private java.math.BigDecimal cashbackAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 20)
  private PromotionApplicationStatus status;

  @Lob
  @Column(name = "REASON")
  private String reason;

  @Column(name = "IDEMPOTENCY_KEY", length = 200, nullable = false)
  private String idempotencyKey;

  @Column(name = "APPLIED_AT", insertable = false, updatable = false)
  private Instant appliedAt;
}
