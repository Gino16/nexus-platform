package io.nexus.catalog.application.port.in;

import io.nexus.catalog.domain.model.Product;
import java.util.Optional;
import java.util.UUID;

public interface GetProductByIdUseCase {

  Optional<Product> getProductById(UUID id);
}
