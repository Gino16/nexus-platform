package io.nexus.order.application;

import io.nexus.order.domain.PurchaseOrder;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class CommercePorts {
  private CommercePorts() {}

  public interface Catalog {
    BigDecimal activePrice(UUID productId);
  }

  public interface Inventory {
    boolean reserve(UUID orderId, List<PurchaseOrder.Item> items);
    void settle(UUID orderId, boolean approved);
  }
}
