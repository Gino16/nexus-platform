package io.nexus.catalog.application.usecase;

import io.nexus.catalog.application.port.in.GetProductByIdUseCase;
import io.nexus.catalog.application.port.out.ProductRepository;
import io.nexus.catalog.domain.model.Product;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class GetProductByIdService implements GetProductByIdUseCase {

  private final ProductRepository productRepository;

  public GetProductByIdService(ProductRepository productRepository) {
    this.productRepository = Objects.requireNonNull(productRepository);
  }

  @Override
  public Optional<Product> getProductById(UUID id) {
    return productRepository.findById(Objects.requireNonNull(id));
  }
}
