package io.nexus.catalog.application.usecase;

import io.nexus.catalog.application.port.in.UpdateProductByIdUseCase;
import io.nexus.catalog.application.port.out.ProductRepository;
import io.nexus.catalog.domain.model.Product;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public class UpdateProductByIdService implements UpdateProductByIdUseCase {

  private final ProductRepository productRepository;

  public UpdateProductByIdService(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  @Override
  public Optional<Product> updateProductById(UUID productId, String name, String description,
      BigDecimal price, boolean active) {
    Product product = new Product(productId, name, description, price, active);
    return productRepository.update(product) ? Optional.of(product) : Optional.empty();
  }
}
