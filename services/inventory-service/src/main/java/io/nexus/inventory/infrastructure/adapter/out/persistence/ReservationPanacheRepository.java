package io.nexus.inventory.infrastructure.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class ReservationPanacheRepository implements PanacheRepositoryBase<ReservationEntity, UUID> {

  boolean insertIfAbsent(UUID orderId, String status) {
    return getEntityManager().createNativeQuery("""
        INSERT INTO reservations (order_id, status) VALUES (:orderId, :status)
        ON CONFLICT (order_id) DO NOTHING
        """)
        .setParameter("orderId", orderId)
        .setParameter("status", status)
        .executeUpdate() == 1;
  }
}
