package io.nexus.inventory.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "stocks")
public class StockEntity {

  @Id
  @Column(name = "product_id")
  UUID productId;

  @Column(nullable = false)
  int available;

  @Column(nullable = false)
  int reserved;

  protected StockEntity() {}
}
