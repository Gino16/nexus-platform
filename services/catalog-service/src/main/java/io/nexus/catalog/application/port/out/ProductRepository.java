package io.nexus.catalog.application.port.out;

import io.nexus.catalog.domain.model.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

  Product save(Product product);

  Optional<Product> findById(UUID id);

  List<Product> findActiveProducts();

  List<Product> findActiveProducts(String name, BigDecimal minPrice, BigDecimal maxPrice,
      int offset, int limit);

  boolean update(Product product);
}
