package io.nexus.catalog.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductTest {

  @Test
  void trimsNameAndNormalizesPriceToTwoDecimalPlaces() {
    Product product = new Product(
        UUID.randomUUID(), "  Coffee  ", "Ground coffee", new BigDecimal("12.5"), true);

    assertEquals("Coffee", product.name());
    assertEquals(new BigDecimal("12.50"), product.price());
  }

  @Test
  void rejectsBlankName() {
    assertThrows(IllegalArgumentException.class,
        () -> new Product(UUID.randomUUID(), "  ", "Description", BigDecimal.ONE, true));
  }

  @Test
  void rejectsNegativePrice() {
    assertThrows(IllegalArgumentException.class,
        () -> new Product(UUID.randomUUID(), "Coffee", "Description", new BigDecimal("-0.01"),
            true));
  }

  @Test
  void rejectsPriceThatCannotBeRepresentedWithTwoDecimalPlaces() {
    assertThrows(IllegalArgumentException.class,
        () -> new Product(UUID.randomUUID(), "Coffee", "Description", new BigDecimal("1.234"),
            true));
  }
}
