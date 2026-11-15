package io.nexus.catalog.application.port.in;

import io.nexus.catalog.domain.model.Product;
import java.math.BigDecimal;

public interface CreateProductUseCase {

  Product createProduct(String name, String description, BigDecimal price);
}
