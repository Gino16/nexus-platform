package io.nexus.order.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseOrder(UUID id, UUID customerId, PaymentOutcome paymentOutcome,
    OffsetDateTime createdAt, Status status, String rejectionReason, List<Line> lines,
    boolean paymentRequested, PaymentOutcome paymentResult) {

  public enum Status { PENDING, PROCESSING, CONFIRMED, REJECTED }
  public enum PaymentOutcome { APPROVED, DECLINED }
  public record Item(UUID productId, int quantity) {}
  public record Line(Item item, BigDecimal unitPrice) {}

  public PurchaseOrder {
    lines = List.copyOf(lines);
  }

  public BigDecimal total() {
    return lines.stream().map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.item().quantity())))
        .reduce(new BigDecimal("0.00"), BigDecimal::add);
  }

  public boolean terminal() {
    return status == Status.CONFIRMED || status == Status.REJECTED;
  }

  public PurchaseOrder transition(Status next, String reason) {
    return new PurchaseOrder(id, customerId, paymentOutcome, createdAt, next, reason, lines,
        paymentRequested, paymentResult);
  }

  public PurchaseOrder requestPayment() {
    return new PurchaseOrder(id, customerId, paymentOutcome, createdAt, status, rejectionReason,
        lines, true, null);
  }
}
