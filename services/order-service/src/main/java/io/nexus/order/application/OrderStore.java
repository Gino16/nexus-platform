package io.nexus.order.application;

import io.nexus.order.domain.PurchaseOrder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

public interface OrderStore {
  Optional<PurchaseOrder> find(UUID id);
  PurchaseOrder insertIfAbsent(PurchaseOrder order);
  List<UUID> pending();
  void advance(UUID id, UnaryOperator<PurchaseOrder> step);
  void defer(UUID id);
}
