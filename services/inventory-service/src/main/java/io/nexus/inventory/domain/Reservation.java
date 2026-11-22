package io.nexus.inventory.domain;

import static io.nexus.inventory.domain.InventoryException.Reason.INVALID_REQUEST;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public record Reservation(UUID orderId, Status status, List<Line> lines) {

  public enum Status { RESERVED, RELEASED, CONFIRMED }

  public record Line(UUID productId, int quantity) {
    public Line {
      if (productId == null || quantity < 1) {
        throw new InventoryException(INVALID_REQUEST, "Each line requires a product ID and a positive quantity");
      }
    }
  }

  public Reservation {
    if (orderId == null || lines == null || lines.isEmpty() || lines.size() > 100) {
      throw new InventoryException(INVALID_REQUEST, "An order ID and 1 to 100 lines are required");
    }
    var products = new HashSet<UUID>();
    for (Line line : lines) {
      if (line == null || !products.add(line.productId())) {
        throw new InventoryException(INVALID_REQUEST, "Each product may appear only once per reservation");
      }
    }
    lines = lines.stream().sorted(java.util.Comparator.comparing(Line::productId)).toList();
  }
}
