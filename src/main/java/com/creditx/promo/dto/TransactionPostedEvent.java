package com.creditx.promo.dto;

import com.creditx.promo.model.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPostedEvent {

  private Long transactionId;
  private TransactionType type;
  private Long issuerAccountId;
  private Long merchantAccountId;
  private BigDecimal amount;
  private String currency;
  private String status;
  private Instant createdAt;
}
