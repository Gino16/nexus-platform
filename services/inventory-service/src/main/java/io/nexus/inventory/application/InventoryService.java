package io.nexus.inventory.application;

import static io.nexus.inventory.domain.InventoryException.Reason.RESERVATION_CONFLICT;
import static io.nexus.inventory.domain.InventoryException.Reason.RESERVATION_NOT_FOUND;
import static io.nexus.inventory.domain.InventoryException.Reason.STOCK_ALREADY_EXISTS;
import static io.nexus.inventory.domain.InventoryException.Reason.STOCK_NOT_FOUND;
import static io.nexus.inventory.domain.Reservation.Status.CONFIRMED;
import static io.nexus.inventory.domain.Reservation.Status.RELEASED;
import static io.nexus.inventory.domain.Reservation.Status.RESERVED;

import io.nexus.inventory.application.port.out.InventoryStore;
import io.nexus.inventory.domain.InventoryException;
import io.nexus.inventory.domain.Reservation;
import io.nexus.inventory.domain.Stock;
import java.util.List;
import java.util.UUID;

public class InventoryService {

  private final InventoryStore store;

  public InventoryService(InventoryStore store) {
    this.store = store;
  }

  public Stock register(UUID productId, int quantity) {
    var stock = new Stock(productId, quantity, 0);
    return store.inTransaction(session -> {
      if (!session.insertStock(stock)) {
        throw new InventoryException(STOCK_ALREADY_EXISTS,
            "Stock is already registered for " + productId);
      }
      return stock;
    });
  }

  public Stock getStock(UUID productId) {
    return store.inTransaction(session -> findStock(session, productId, false));
  }

  public Stock adjust(UUID productId, int delta) {
    return store.inTransaction(session -> {
      Stock adjusted = findStock(session, productId, true).adjust(delta);
      session.updateStock(adjusted);
      return adjusted;
    });
  }

  public Reservation reserve(UUID orderId, List<Reservation.Line> lines) {
    var requested = new Reservation(orderId, RESERVED, lines);
    return store.inTransaction(session -> {
      if (!session.insertReservation(requested)) {
        Reservation existing = findReservation(session, orderId, true);
        if (!existing.lines().equals(requested.lines())) {
          throw new InventoryException(RESERVATION_CONFLICT,
              "The order already has different reservation lines");
        }
        return existing;
      }
      for (Reservation.Line line : requested.lines()) {
        Stock stock = findStock(session, line.productId(), true);
        session.updateStock(stock.reserve(line.quantity()));
      }
      session.insertReservationLines(requested);
      return requested;
    });
  }

  public Reservation getReservation(UUID orderId) {
    return store.inTransaction(session -> findReservation(session, orderId, false));
  }

  public Reservation release(UUID orderId) {
    return settle(orderId, RELEASED);
  }

  public Reservation confirm(UUID orderId) {
    return settle(orderId, CONFIRMED);
  }

  private Reservation settle(UUID orderId, Reservation.Status target) {
    return store.inTransaction(session -> {
      Reservation reservation = findReservation(session, orderId, true);
      if (reservation.status() == target) {
        return reservation;
      }
      if (reservation.status() != RESERVED) {
        throw new InventoryException(RESERVATION_CONFLICT,
            "The reservation is already " + reservation.status());
      }

      for (Reservation.Line line : reservation.lines()) {
        Stock stock = findStock(session, line.productId(), true);
        session.updateStock(stock.settle(line.quantity(), target == RELEASED));
      }
      var settled = new Reservation(orderId, target, reservation.lines());
      session.updateReservationStatus(settled);
      return settled;
    });
  }

  private static Stock findStock(InventoryStore.Session session, UUID productId, boolean lock) {
    return session.findStock(productId, lock).orElseThrow(
        () -> new InventoryException(STOCK_NOT_FOUND, "Stock is not registered for " + productId));
  }

  private static Reservation findReservation(InventoryStore.Session session, UUID orderId,
      boolean lock) {
    return session.findReservation(orderId, lock).orElseThrow(
        () -> new InventoryException(RESERVATION_NOT_FOUND,
            "Reservation not found for " + orderId));
  }
}
