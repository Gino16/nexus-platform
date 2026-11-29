package io.nexus.payment.infrastructure;

import io.nexus.payment.application.AuthorizePayment;
import io.nexus.payment.domain.Authorization;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.UUID;
import java.util.function.Consumer;

@ApplicationScoped
public class AuthorizationStore implements AuthorizePayment.Store,
    PanacheRepositoryBase<AuthorizationEntity, UUID> {

  @Override
  @Transactional
  public void saveIfConsistent(Authorization authorization) {
    int inserted = getEntityManager().createNativeQuery("""
        INSERT INTO payment_authorizations (order_id, amount, currency, outcome)
        VALUES (:id, :amount, :currency, :outcome)
        ON CONFLICT (order_id) DO NOTHING
        """).setParameter("id", authorization.orderId()).setParameter("amount", authorization.amount())
        .setParameter("currency", authorization.currency())
        .setParameter("outcome", authorization.outcome().name()).executeUpdate();

    if (inserted == 0 && !toDomain(findById(authorization.orderId())).equals(authorization)) {
      throw new IllegalArgumentException("Conflicting authorization for order " + authorization.orderId());
    }
  }

  @Transactional
  public boolean publishNext(Consumer<Authorization> publisher) {
    var next = getEntityManager().createNativeQuery("""
        SELECT * FROM payment_authorizations WHERE published = FALSE
        ORDER BY authorized_at, order_id LIMIT 1 FOR UPDATE SKIP LOCKED
        """, AuthorizationEntity.class).getResultList();
    if (next.isEmpty()) {
      return false;
    }
    var entity = (AuthorizationEntity) next.getFirst();
    publisher.accept(toDomain(entity));
    entity.published = true;
    return true;
  }

  private static Authorization toDomain(AuthorizationEntity entity) {
    return new Authorization(entity.orderId, entity.amount, entity.currency,
        Authorization.Outcome.valueOf(entity.outcome));
  }
}
