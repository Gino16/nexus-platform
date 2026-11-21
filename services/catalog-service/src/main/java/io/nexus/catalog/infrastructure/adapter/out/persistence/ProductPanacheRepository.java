package io.nexus.catalog.infrastructure.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProductPanacheRepository implements PanacheRepositoryBase<ProductEntity, UUID> {

  public List<ProductEntity> findActiveProducts() {
    return list("active = true order by name, id");
  }
}
