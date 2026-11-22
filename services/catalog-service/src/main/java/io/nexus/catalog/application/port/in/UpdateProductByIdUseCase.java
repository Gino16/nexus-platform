package io.nexus.catalog.application.port.in;

import io.nexus.catalog.domain.model.Product;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface UpdateProductByIdUseCase {

  Optional<Product> updateProductById(UUID productId, String name, String description,
      BigDecimal price, boolean active);
}
