package io.nexus.catalog.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * A catalog product, independent of persistence and delivery frameworks.
 */
public record Product(UUID id, String name, String description, BigDecimal price, boolean active) {

  public Product {
    Objects.requireNonNull(id, "id must not be null");
    name = requireName(name);
    Objects.requireNonNull(description, "description must not be null");
    price = normalizePrice(price);
  }

  private static String requireName(String name) {
    Objects.requireNonNull(name, "name must not be null");
    String normalizedName = name.trim();
    if (normalizedName.isEmpty()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    return normalizedName;
  }

  private static BigDecimal normalizePrice(BigDecimal price) {
    Objects.requireNonNull(price, "price must not be null");

    BigDecimal normalizedPrice;
    try {
      normalizedPrice = price.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException("price must have at most two decimal places", exception);
    }

    if (normalizedPrice.signum() < 0) {
      throw new IllegalArgumentException("price must not be negative");
    }
    return normalizedPrice;
  }
}
