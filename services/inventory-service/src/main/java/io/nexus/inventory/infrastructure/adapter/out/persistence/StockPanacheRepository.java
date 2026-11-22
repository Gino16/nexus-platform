package io.nexus.inventory.infrastructure.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class StockPanacheRepository implements PanacheRepositoryBase<StockEntity, UUID> {

  boolean insertIfAbsent(UUID productId, int available, int reserved) {
    return getEntityManager().createNativeQuery("""
        INSERT INTO stocks (product_id, available, reserved) VALUES (:productId, :available, :reserved)
        ON CONFLICT (product_id) DO NOTHING
        """)
        .setParameter("productId", productId)
        .setParameter("available", available)
        .setParameter("reserved", reserved)
        .executeUpdate() == 1;
  }
}
