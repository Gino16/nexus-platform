package io.nexus.order.application;

import io.nexus.order.application.OrderFailure.Reason;
import io.nexus.order.domain.PurchaseOrder;
import io.nexus.order.domain.PurchaseOrder.Item;
import io.nexus.order.domain.PurchaseOrder.PaymentOutcome;
import io.nexus.order.domain.PurchaseOrder.Status;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class OrderService {
  private final OrderStore store;
  private final CommercePorts.Catalog catalog;
  private final CommercePorts.Inventory inventory;

  public OrderService(OrderStore store, CommercePorts.Catalog catalog,
      CommercePorts.Inventory inventory) {
    this.store = store;
    this.catalog = catalog;
    this.inventory = inventory;
  }

  public PurchaseOrder submit(UUID id, UUID customer, PaymentOutcome payment, List<Item> requested) {
    if (id == null || customer == null || payment == null || requested == null || requested.isEmpty()
        || requested.size() > 100 || requested.stream().anyMatch(
            item -> item == null || item.productId() == null || item.quantity() <= 0)) {
      throw new OrderFailure(Reason.INVALID_ORDER, "Invalid order lines or identifiers");
    }
    if (requested.stream().map(Item::productId).distinct().count() != requested.size()) {
      throw new OrderFailure(Reason.INVALID_ORDER, "A product may appear only once");
    }
    var items = requested.stream().sorted(Comparator.comparing(Item::productId)).toList();
    var existing = store.find(id);
    if (existing.isPresent()) {
      return verifyRetry(existing.get(), customer, payment, items);
    }
    var lines = items.stream()
        .map(item -> new PurchaseOrder.Line(item, catalog.activePrice(item.productId()))).toList();
    var order = new PurchaseOrder(id, customer, payment, OffsetDateTime.now(ZoneOffset.UTC),
        Status.PENDING, null, lines, false, null);
    return verifyRetry(store.insertIfAbsent(order), customer, payment, items);
  }

  public PurchaseOrder get(UUID id) {
    return store.find(id).orElseThrow(() -> new OrderFailure(Reason.ORDER_NOT_FOUND, "Order not found"));
  }

  public void process(UUID id) {
    store.advance(id, order -> {
      if (order.terminal()) {
        return order;
      }
      if (order.status() == Status.PENDING) {
        return order.transition(Status.PROCESSING, null);
      }
      if (!order.paymentRequested()) {
        if (!inventory.reserve(id, order.lines().stream().map(PurchaseOrder.Line::item).toList())) {
          return order.transition(Status.REJECTED, "INVENTORY_UNAVAILABLE");
        }
        return order.requestPayment();
      }
      if (order.paymentResult() == null) {
        return order;
      }
      boolean approved = order.paymentResult() == PaymentOutcome.APPROVED;
      inventory.settle(id, approved);
      return approved ? order.transition(Status.CONFIRMED, null)
          : order.transition(Status.REJECTED, "PAYMENT_DECLINED");
    });
  }

  private static PurchaseOrder verifyRetry(PurchaseOrder existing, UUID customer,
      PaymentOutcome payment, List<Item> items) {
    if (!existing.customerId().equals(customer) || existing.paymentOutcome() != payment
        || !existing.lines().stream().map(PurchaseOrder.Line::item).toList().equals(items)) {
      throw new OrderFailure(Reason.ORDER_CONFLICT, "Order ID already used for a different request");
    }
    return existing;
  }
}
