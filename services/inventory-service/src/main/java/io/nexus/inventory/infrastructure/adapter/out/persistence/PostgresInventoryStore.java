package io.nexus.inventory.infrastructure.adapter.out.persistence;

import io.nexus.inventory.application.port.out.InventoryStore;
import io.nexus.inventory.domain.Reservation;
import io.nexus.inventory.domain.Stock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@ApplicationScoped
public class PostgresInventoryStore implements InventoryStore {

  private final StockPanacheRepository stocks;
  private final ReservationPanacheRepository reservations;

  @Inject
  public PostgresInventoryStore(StockPanacheRepository stocks,
      ReservationPanacheRepository reservations) {
    this.stocks = stocks;
    this.reservations = reservations;
  }

  @Override
  @Transactional
  public <T> T inTransaction(Function<Session, T> work) {
    return work.apply(new PanacheSession(stocks, reservations));
  }

  private record PanacheSession(StockPanacheRepository stocks,
                                ReservationPanacheRepository reservations) implements Session {

    @Override
    public boolean insertStock(Stock stock) {
      return stocks.insertIfAbsent(stock.productId(), stock.available(), stock.reserved());
    }

    @Override
    public Optional<Stock> findStock(UUID productId, boolean lock) {
      return stocks.findByIdOptional(productId, lockMode(lock))
          .map(entity -> new Stock(entity.productId, entity.available, entity.reserved));
    }

    @Override
    public void updateStock(Stock stock) {
      StockEntity entity = stocks.findById(stock.productId());
      entity.available = stock.available();
      entity.reserved = stock.reserved();
    }

    @Override
    public boolean insertReservation(Reservation reservation) {
      return reservations.insertIfAbsent(reservation.orderId(), reservation.status().name());
    }

    @Override
    public void insertReservationLines(Reservation reservation) {
      ReservationEntity entity = reservations.findById(reservation.orderId());
      for (Reservation.Line line : reservation.lines()) {
        entity.lines.add(new ReservationEntity.Line(line.productId(), line.quantity()));
      }
    }

    @Override
    public Optional<Reservation> findReservation(UUID orderId, boolean lock) {
      return reservations.findByIdOptional(orderId, lockMode(lock))
          .map(entity -> new Reservation(entity.orderId, Reservation.Status.valueOf(entity.status),
              entity.lines.stream()
                  .map(line -> new Reservation.Line(line.productId(), line.quantity()))
                  .toList()));
    }

    @Override
    public void updateReservationStatus(Reservation reservation) {
      reservations.findById(reservation.orderId()).status = reservation.status().name();
    }

    private static LockModeType lockMode(boolean lock) {
      return lock ? LockModeType.PESSIMISTIC_WRITE : LockModeType.NONE;
    }
  }
}
