package io.nexus.catalog.infrastructure.adapter.out.persistence;

import io.nexus.catalog.application.port.out.ProductRepository;
import io.nexus.catalog.domain.model.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PostgresProductRepository implements ProductRepository {

  private final ProductPanacheRepository panacheRepository;

  @Inject
  public PostgresProductRepository(ProductPanacheRepository panacheRepository) {
    this.panacheRepository = panacheRepository;
  }

  @Override
  @Transactional
  public Product save(Product product) {
    ProductEntity mergedEntity = panacheRepository.getEntityManager()
        .merge(ProductPersistenceMapper.toEntity(product));
    return ProductPersistenceMapper.toDomain(mergedEntity);
  }

  @Override
  public Optional<Product> findById(UUID id) {
    return panacheRepository.findByIdOptional(id).map(ProductPersistenceMapper::toDomain);
  }

  @Override
  public List<Product> findActiveProducts() {
    return panacheRepository.findActiveProducts().stream()
        .map(ProductPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public List<Product> findActiveProducts(String name, BigDecimal minPrice, BigDecimal maxPrice,
      int offset, int limit) {
    return panacheRepository.findActiveProducts(name, minPrice, maxPrice, offset, limit).stream()
        .map(ProductPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional
  public boolean update(Product product) {
    return panacheRepository.update("name = ?1, description = ?2, price = ?3, active = ?4 where id = ?5",
        product.name(), product.description(), product.price(), product.active(), product.id()) == 1;
  }
}
