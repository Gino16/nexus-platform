package io.nexus.inventory.domain;

import static io.nexus.inventory.domain.InventoryException.Reason.*;

import java.util.UUID;

public record Stock(UUID productId, int available, int reserved) {

  public Stock {
    if (productId == null || available < 0 || reserved < 0) {
      throw new InventoryException(INVALID_REQUEST, "Product ID and nonnegative quantities are required");
    }
    if ((long) available + reserved > Integer.MAX_VALUE) {
      throw new InventoryException(STOCK_LIMIT_EXCEEDED, "Stock limit exceeded for " + productId);
    }
  }

  public Stock adjust(int delta) {
    if (delta == 0) {
      throw new InventoryException(INVALID_REQUEST, "delta must be nonzero");
    }
    long adjusted = (long) available + delta;
    if (adjusted < 0) {
      throw new InventoryException(INSUFFICIENT_STOCK, "Insufficient available stock for " + productId);
    }
    if (adjusted + reserved > Integer.MAX_VALUE) {
      throw new InventoryException(STOCK_LIMIT_EXCEEDED, "Stock limit exceeded for " + productId);
    }
    return new Stock(productId, (int) adjusted, reserved);
  }

  public Stock reserve(int quantity) {
    if (quantity > available) {
      throw new InventoryException(INSUFFICIENT_STOCK, "Insufficient available stock for " + productId);
    }
    return new Stock(productId, available - quantity, reserved + quantity);
  }

  public Stock settle(int quantity, boolean release) {
    if (quantity > reserved) {
      throw new IllegalStateException("Reservation exceeds reserved stock for " + productId);
    }
    return new Stock(productId, release ? available + quantity : available, reserved - quantity);
  }
}
