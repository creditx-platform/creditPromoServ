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
@Table(name = "CPRS_PROMOTIONS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

  @Id
  @Column(name = "PROMO_ID", length = 36)
  private String promoId;

  @Column(name = "NAME", nullable = false, length = 150)
  private String name;

  @Column(name = "DESCRIPTION")
  private String description;

  @Column(name = "START_DATE", nullable = false)
  private Instant startDate;

  @Column(name = "EXPIRY_DATE", nullable = false)
  private Instant expiryDate;

  @Lob
  @Column(name = "ELIGIBILITY_RULES", nullable = false)
  private String eligibilityRules;

  @Lob
  @Column(name = "REWARD_FORMULA", nullable = false)
  private String rewardFormula;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 20)
  private PromotionStatus status;

  @Column(name = "CREATED_AT", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "UPDATED_AT", insertable = false, updatable = false)
  private Instant updatedAt;
}
