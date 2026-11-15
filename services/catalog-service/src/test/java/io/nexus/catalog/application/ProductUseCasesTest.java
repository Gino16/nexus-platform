package io.nexus.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nexus.catalog.application.port.out.ProductRepository;
import io.nexus.catalog.application.usecase.CreateProductService;
import io.nexus.catalog.application.usecase.GetProductByIdService;
import io.nexus.catalog.application.usecase.ListActiveProductsService;
import io.nexus.catalog.domain.model.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductUseCasesTest {

  @Mock
  ProductRepository productRepository;

  @InjectMocks
  CreateProductService createProductService;
  @InjectMocks
  GetProductByIdService getProductByIdService;
  @InjectMocks
  ListActiveProductsService listActiveProductsService;

  @Test
  void createsAnActiveProductAndSavesIt() {
    // Arrange
    when(productRepository.save(any(Product.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Product created = createProductService.createProduct(
        "Coffee", "Ground coffee", new BigDecimal("8.50"));

    // Assert
    assertNotNull(created.id());
    assertTrue(created.active());
    assertEquals("Coffee", created.name());
    assertEquals("Ground coffee", created.description());
    assertEquals(new BigDecimal("8.50"), created.price());
    verify(productRepository).save(created);
  }

  @Test
  void returnsProductWhenItsIdExists() {
    // Arrange
    Product existing = sampleProduct();
    when(productRepository.findById(existing.id())).thenReturn(Optional.of(existing));

    // Act
    Optional<Product> result = getProductByIdService.getProductById(existing.id());

    // Assert
    assertEquals(Optional.of(existing), result);
    verify(productRepository).findById(existing.id());
  }

  @Test
  void returnsEmptyWhenProductIdDoesNotExist() {
    // Arrange
    UUID id = UUID.randomUUID();
    when(productRepository.findById(id)).thenReturn(Optional.empty());

    // Act
    Optional<Product> result = getProductByIdService.getProductById(id);

    // Assert
    assertTrue(result.isEmpty());
    verify(productRepository).findById(id);
  }

  @Test
  void listsProductsFromTheActiveProductQuery() {
    // Arrange
    List<Product> activeProducts = List.of(sampleProduct());
    when(productRepository.findActiveProducts()).thenReturn(activeProducts);

    // Act
    List<Product> result = listActiveProductsService.listActiveProducts();

    // Assert
    assertEquals(activeProducts, result);
    verify(productRepository).findActiveProducts();
  }

  private static Product sampleProduct() {
    return new Product(
        UUID.randomUUID(), "Coffee", "Ground coffee", new BigDecimal("8.50"), true);
  }
}
