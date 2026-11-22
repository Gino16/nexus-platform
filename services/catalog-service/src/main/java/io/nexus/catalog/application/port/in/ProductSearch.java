package io.nexus.catalog.application.port.in;

import java.math.BigDecimal;

public record ProductSearch(int page, int size, String name, BigDecimal minPrice, BigDecimal maxPrice) {

  public ProductSearch {
    if (page < 0 || page > 1_000_000 || size < 1 || size > 100) {
      throw new IllegalArgumentException("Invalid page or size");
    }
    name = name == null ? "" : name.strip();
    if (name.length() > 100) {
      throw new IllegalArgumentException("name: must have at most 100 characters");
    }
    if ((minPrice != null && minPrice.signum() < 0)
        || (maxPrice != null && maxPrice.signum() < 0)) {
      throw new IllegalArgumentException("Price filters must not be negative");
    }
    if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
      throw new IllegalArgumentException("maxPrice: must be greater than or equal to minPrice");
    }
  }
}
