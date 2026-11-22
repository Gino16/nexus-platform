package io.nexus.inventory.domain;

public final class InventoryException extends RuntimeException {

  public enum Reason {
    INVALID_REQUEST, STOCK_NOT_FOUND, STOCK_ALREADY_EXISTS, INSUFFICIENT_STOCK,
    STOCK_LIMIT_EXCEEDED, RESERVATION_NOT_FOUND, RESERVATION_CONFLICT
  }

  private final Reason reason;

  public InventoryException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }
}
