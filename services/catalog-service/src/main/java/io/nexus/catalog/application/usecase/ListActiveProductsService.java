package io.nexus.catalog.application.usecase;

import io.nexus.catalog.application.port.in.ListActiveProductsUseCase;
import io.nexus.catalog.application.port.in.ProductPage;
import io.nexus.catalog.application.port.in.ProductSearch;
import io.nexus.catalog.application.port.out.ProductRepository;
import io.nexus.catalog.domain.model.Product;
import java.util.List;
import java.util.Objects;

public class ListActiveProductsService implements ListActiveProductsUseCase {

  private final ProductRepository productRepository;

  public ListActiveProductsService(ProductRepository productRepository) {
    this.productRepository = Objects.requireNonNull(productRepository);
  }

  @Override
  public List<Product> listActiveProducts() {
    return List.copyOf(productRepository.findActiveProducts());
  }

  @Override
  public ProductPage listActiveProducts(ProductSearch search) {
    List<Product> products = productRepository.findActiveProducts(search.name(), search.minPrice(),
        search.maxPrice(), search.page() * search.size(), search.size() + 1);
    boolean hasNext = products.size() > search.size();
    return new ProductPage(products.subList(0, Math.min(search.size(), products.size())), hasNext);
  }
}
