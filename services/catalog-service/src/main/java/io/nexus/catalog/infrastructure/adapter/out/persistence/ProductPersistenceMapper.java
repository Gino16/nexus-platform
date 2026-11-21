package io.nexus.catalog.infrastructure.adapter.out.persistence;

import io.nexus.catalog.domain.model.Product;

final class ProductPersistenceMapper {

  private ProductPersistenceMapper() {}

  static ProductEntity toEntity(Product product) {
    return new ProductEntity(
        product.id(),
        product.name(),
        product.description(),
        product.price(),
        product.active());
  }

  static Product toDomain(ProductEntity entity) {
    return new Product(
        entity.id,
        entity.name,
        entity.description,
        entity.price,
        entity.active);
  }
}
