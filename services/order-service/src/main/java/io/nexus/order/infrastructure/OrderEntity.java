package io.nexus.order.infrastructure;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "purchase_orders")
public class OrderEntity {
  @Id UUID id;
  @Column(name = "customer_id") UUID customerId;
  @Column(name = "payment_outcome", columnDefinition = "text") String paymentOutcome;
  @Column(columnDefinition = "text") String status;
  @Column(name = "rejection_reason", columnDefinition = "text") String rejectionReason;
  @Column(name = "created_at") OffsetDateTime createdAt;
  @Column(name = "next_attempt") OffsetDateTime nextAttempt;
  @Column(name = "payment_requested") boolean paymentRequested;
  @Column(name = "payment_result", columnDefinition = "text") String paymentResult;

  @ElementCollection
  @CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
  Set<Line> lines = new HashSet<>();

  @Embeddable
  public record Line(@Column(name = "product_id") UUID productId, int quantity,
      @Column(name = "unit_price", precision = 19, scale = 2) BigDecimal unitPrice) {}
}
