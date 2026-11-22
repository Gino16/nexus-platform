package io.nexus.inventory.infrastructure.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class ReservationEntity {

  @Id
  @Column(name = "order_id")
  UUID orderId;

  @Column(nullable = false, columnDefinition = "text")
  String status;

  @ElementCollection
  @CollectionTable(name = "reservation_lines", joinColumns = @JoinColumn(name = "order_id"))
  Set<Line> lines = new HashSet<>();

  protected ReservationEntity() {}

  @Embeddable
  public record Line(
      @Column(name = "product_id", nullable = false) UUID productId,
      @Column(nullable = false) int quantity) {}
}
