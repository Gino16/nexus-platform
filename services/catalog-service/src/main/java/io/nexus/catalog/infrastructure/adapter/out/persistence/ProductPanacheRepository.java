package io.nexus.catalog.infrastructure.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ProductPanacheRepository implements PanacheRepositoryBase<ProductEntity, UUID> {

  public List<ProductEntity> findActiveProducts() {
    return list("active = true order by name, id");
  }

  public List<ProductEntity> findActiveProducts(String name, BigDecimal minPrice, BigDecimal maxPrice,
      int offset, int limit) {
    StringBuilder query = new StringBuilder("active = true");
    Map<String, Object> parameters = new HashMap<>();
    if (!name.isEmpty()) {
      query.append(" and locate(lower(:name), lower(name)) > 0");
      parameters.put("name", name);
    }
    if (minPrice != null) {
      query.append(" and price >= :minPrice");
      parameters.put("minPrice", minPrice);
    }
    if (maxPrice != null) {
      query.append(" and price <= :maxPrice");
      parameters.put("maxPrice", maxPrice);
    }
    query.append(" order by name, id");
    return find(query.toString(), parameters).range(offset, offset + limit - 1).list();
  }
}
