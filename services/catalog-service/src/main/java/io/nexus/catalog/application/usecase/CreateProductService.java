package io.nexus.catalog.application.usecase;

import io.nexus.catalog.application.port.in.CreateProductUseCase;
import io.nexus.catalog.application.port.out.ProductRepository;
import io.nexus.catalog.domain.model.Product;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class CreateProductService implements CreateProductUseCase {

  private final ProductRepository productRepository;

  public CreateProductService(ProductRepository productRepository) {
    this.productRepository = Objects.requireNonNull(productRepository);
  }

  @Override
  public Product createProduct(String name, String description, BigDecimal price) {
    Product product = new Product(UUID.randomUUID(), name, description, price, true);
    return productRepository.save(product);
  }
}
