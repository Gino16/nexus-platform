package io.nexus.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nexus.catalog.application.port.out.ProductRepository;
import io.nexus.catalog.application.usecase.CreateProductService;
import io.nexus.catalog.application.usecase.GetProductByIdService;
import io.nexus.catalog.application.usecase.ListActiveProductsService;
import io.nexus.catalog.domain.model.Product;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductUseCasesTest {

  @Test
  void createsAnActiveProductAndSavesIt() {
    InMemoryProductRepository repository = new InMemoryProductRepository();
    CreateProductService createProduct = new CreateProductService(repository);

    Product created = createProduct.createProduct("Coffee", "Ground coffee", new BigDecimal("8.50"));

    assertNotNull(created.id());
    assertTrue(created.active());
    assertEquals("Coffee", created.name());
    assertEquals("Ground coffee", created.description());
    assertEquals(new BigDecimal("8.50"), created.price());
    assertEquals(Optional.of(created), repository.findById(created.id()));
  }

  @Test
  void returnsProductWhenItsIdExists() {
    InMemoryProductRepository repository = new InMemoryProductRepository();
    Product existing = product(true);
    repository.save(existing);
    GetProductByIdService getProduct = new GetProductByIdService(repository);

    assertEquals(Optional.of(existing), getProduct.getProductById(existing.id()));
  }

  @Test
  void returnsEmptyWhenProductIdDoesNotExist() {
    GetProductByIdService getProduct = new GetProductByIdService(new InMemoryProductRepository());

    assertTrue(getProduct.getProductById(UUID.randomUUID()).isEmpty());
  }

  @Test
  void listsOnlyProductsReturnedByTheActiveProductRepositoryQuery() {
    InMemoryProductRepository repository = new InMemoryProductRepository();
    Product active = product(true);
    Product inactive = product(false);
    repository.save(active);
    repository.save(inactive);
    ListActiveProductsService listActiveProducts = new ListActiveProductsService(repository);

    List<Product> result = listActiveProducts.listActiveProducts();

    assertEquals(List.of(active), result);
    assertFalse(result.contains(inactive));
  }

  private static Product product(boolean active) {
    return new Product(UUID.randomUUID(), "Coffee", "Ground coffee", new BigDecimal("8.50"), active);
  }

  private static class InMemoryProductRepository implements ProductRepository {

    private final List<Product> products = new ArrayList<>();

    @Override
    public Product save(Product product) {
      products.add(product);
      return product;
    }

    @Override
    public Optional<Product> findById(UUID id) {
      return products.stream().filter(product -> product.id().equals(id)).findFirst();
    }

    @Override
    public List<Product> findActiveProducts() {
      return products.stream().filter(Product::active).toList();
    }
  }
}
