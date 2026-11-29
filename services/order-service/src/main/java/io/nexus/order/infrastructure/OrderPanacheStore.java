package io.nexus.order.infrastructure;

import io.nexus.order.application.OrderStore;
import io.nexus.order.domain.PurchaseOrder;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

@ApplicationScoped
public class OrderPanacheStore implements OrderStore, PanacheRepositoryBase<OrderEntity, UUID> {

  @Override
  @Transactional
  public Optional<PurchaseOrder> find(UUID id) {
    return findByIdOptional(id).map(OrderPanacheStore::toDomain);
  }

  @Override
  @Transactional
  public PurchaseOrder insertIfAbsent(PurchaseOrder order) {
    int inserted = getEntityManager().createNativeQuery("""
        INSERT INTO purchase_orders (id, customer_id, payment_outcome, status, created_at)
        VALUES (:id, :customer, :payment, 'PENDING', :created)
        ON CONFLICT (id) DO NOTHING
        """).setParameter("id", order.id()).setParameter("customer", order.customerId())
        .setParameter("payment", order.paymentOutcome().name())
        .setParameter("created", order.createdAt()).executeUpdate();
    OrderEntity entity = findById(order.id());
    if (inserted == 1) {
      order.lines().forEach(line -> entity.lines.add(
          new OrderEntity.Line(line.item().productId(), line.item().quantity(), line.unitPrice())));
      return order;
    }
    return toDomain(entity);
  }

  @Override
  @Transactional
  public List<UUID> pending() {
    return find("""
        status in ('PENDING','PROCESSING') and nextAttempt <= ?1
        and (paymentRequested = false or paymentResult is not null) order by nextAttempt, id
        """,
        OffsetDateTime.now()).page(0, 20).list().stream().map(entity -> entity.id).toList();
  }

  @Override
  @Transactional
  public void advance(UUID id, UnaryOperator<PurchaseOrder> step) {
    var entity = findById(id, LockModeType.PESSIMISTIC_WRITE);
    var previous = toDomain(entity);
    var next = step.apply(previous);
    entity.status = next.status().name();
    entity.rejectionReason = next.rejectionReason();
    entity.paymentRequested = next.paymentRequested();
    if (next.paymentRequested() && !previous.paymentRequested()) {
      enqueue(id, "PaymentRequested");
    }
    if (next.terminal() && !previous.terminal()) {
      enqueue(id, next.status() == PurchaseOrder.Status.CONFIRMED ? "OrderConfirmed" : "OrderRejected");
    }
  }

  private void enqueue(UUID id, String type) {
    getEntityManager().createNativeQuery("""
        INSERT INTO order_outbox (order_id, event_type) VALUES (:id, :event)
        """).setParameter("id", id).setParameter("event", type).executeUpdate();
  }

  @Transactional
  public void recordPayment(UUID id, BigDecimal amount, PurchaseOrder.PaymentOutcome outcome) {
    var entity = findById(id, LockModeType.PESSIMISTIC_WRITE);
    if (entity == null || !entity.paymentRequested || toDomain(entity).total().compareTo(amount) != 0) {
      throw new IllegalArgumentException("Payment result does not match a requested order: " + id);
    }
    if (entity.paymentResult != null && !entity.paymentResult.equals(outcome.name())) {
      throw new IllegalArgumentException("Conflicting payment result for " + id);
    }
    entity.paymentResult = outcome.name();
  }

  @Override
  @Transactional
  public void defer(UUID id) {
    update("nextAttempt = ?1 where id = ?2", OffsetDateTime.now().plusSeconds(5), id);
  }

  @Transactional
  public boolean publishNext(KafkaOrderEvents publisher) {
    var rows = getEntityManager().unwrap(org.hibernate.Session.class).createNativeQuery("""
        SELECT order_id, event_type, CAST(EXTRACT(EPOCH FROM created_at) * 1000 AS bigint)
        FROM order_outbox
        WHERE published = FALSE ORDER BY created_at, order_id LIMIT 1 FOR UPDATE SKIP LOCKED
        """, Object[].class).getResultList();
    if (rows.isEmpty()) {
      return false;
    }

    var row = rows.getFirst();
    var id = (UUID) row[0];
    var timestamp = java.time.Instant.ofEpochMilli(((Number) row[2]).longValue())
        .atOffset(java.time.ZoneOffset.UTC);
    var type = (String) row[1];
    if ("PaymentRequested".equals(type)) {
      publisher.requestPayment(toDomain(findById(id)));
    } else {
      publisher.publish(new OrderEvent(id, id, type, timestamp, 1));
    }
    getEntityManager().createNativeQuery("""
        UPDATE order_outbox SET published = TRUE WHERE order_id = :id AND event_type = :type
        """).setParameter("id", id).setParameter("type", type).executeUpdate();
    return true;
  }

  private static PurchaseOrder toDomain(OrderEntity entity) {
    var lines = entity.lines.stream().sorted(Comparator.comparing(OrderEntity.Line::productId))
        .map(line -> new PurchaseOrder.Line(new PurchaseOrder.Item(line.productId(), line.quantity()),
            line.unitPrice())).toList();
    return new PurchaseOrder(entity.id, entity.customerId,
        PurchaseOrder.PaymentOutcome.valueOf(entity.paymentOutcome), entity.createdAt,
        PurchaseOrder.Status.valueOf(entity.status), entity.rejectionReason, lines,
        entity.paymentRequested, entity.paymentResult == null ? null
            : PurchaseOrder.PaymentOutcome.valueOf(entity.paymentResult));
  }
}
