package io.nexus.catalog.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductEntity {

  @Id
  public UUID id;

  @Column(nullable = false, columnDefinition = "text")
  public String name;

  @Column(nullable = false, columnDefinition = "text")
  public String description;

  @Column(nullable = false, precision = 19, scale = 2)
  public BigDecimal price;

  @Column(nullable = false)
  public boolean active;

  protected ProductEntity() {}

  ProductEntity(UUID id, String name, String description, BigDecimal price, boolean active) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.price = price;
    this.active = active;
  }
}
