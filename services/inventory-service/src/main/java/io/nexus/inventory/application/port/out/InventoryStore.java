package io.nexus.inventory.application.port.out;

import io.nexus.inventory.domain.Reservation;
import io.nexus.inventory.domain.Stock;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public interface InventoryStore {

  <T> T inTransaction(Function<Session, T> work);

  interface Session {

    boolean insertStock(Stock stock);

    Optional<Stock> findStock(UUID productId, boolean lock);

    void updateStock(Stock stock);

    boolean insertReservation(Reservation reservation);

    void insertReservationLines(Reservation reservation);

    Optional<Reservation> findReservation(UUID orderId, boolean lock);

    void updateReservationStatus(Reservation reservation);
  }
}
