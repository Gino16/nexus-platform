package io.nexus.catalog.application.port.in;

import io.nexus.catalog.domain.model.Product;
import java.util.List;

public record ProductPage(List<Product> products, boolean hasNext) {

  public ProductPage {
    products = List.copyOf(products);
  }
}
