package io.nexus.payment.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_authorizations")
public class AuthorizationEntity {
  @Id @Column(name = "order_id")
  UUID orderId;

  @Column(precision = 31, scale = 2)
  BigDecimal amount;

  @Column(columnDefinition = "text")
  String currency;

  @Column(columnDefinition = "text")
  String outcome;

  @Column(name = "authorized_at")
  OffsetDateTime authorizedAt;

  boolean published;
}
