package io.nexus.order.application;

public final class OrderFailure extends RuntimeException {
  public enum Reason { INVALID_ORDER, ORDER_NOT_FOUND, ORDER_CONFLICT, DEPENDENCY_UNAVAILABLE }
  private final Reason reason;

  public OrderFailure(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public OrderFailure(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }
}
