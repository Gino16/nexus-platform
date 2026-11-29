package io.nexus.payment.application;

import io.nexus.payment.domain.Authorization;
import io.nexus.payment.domain.Authorization.Outcome;
import java.math.BigDecimal;
import java.util.UUID;

public class AuthorizePayment {

  public interface Store {
    void saveIfConsistent(Authorization authorization);
  }

  private final Store store;

  public AuthorizePayment(Store store) {
    this.store = store;
  }

  public void authorize(UUID orderId, BigDecimal amount, String currency, Outcome requestedOutcome) {
    store.saveIfConsistent(new Authorization(orderId, amount, currency, requestedOutcome));
  }
}
