package io.nexus.payment.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record Authorization(UUID orderId, BigDecimal amount, String currency, Outcome outcome) {

  public enum Outcome { APPROVED, DECLINED }

  public Authorization {
    if (orderId == null || amount == null || amount.signum() < 0
        || !"PEN".equals(currency) || outcome == null) {
      throw new IllegalArgumentException("Invalid simulated authorization");
    }
    amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    if (amount.precision() > 31) {
      throw new IllegalArgumentException("Authorization amount exceeds supported precision");
    }
  }
}
