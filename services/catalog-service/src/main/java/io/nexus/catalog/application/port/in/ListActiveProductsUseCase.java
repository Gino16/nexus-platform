package io.nexus.catalog.application.port.in;

import io.nexus.catalog.domain.model.Product;
import java.util.List;

public interface ListActiveProductsUseCase {

  List<Product> listActiveProducts();
}
