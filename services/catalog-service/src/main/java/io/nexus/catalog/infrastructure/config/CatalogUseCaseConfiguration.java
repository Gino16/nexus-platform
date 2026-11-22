package io.nexus.catalog.infrastructure.config;

import io.nexus.catalog.application.port.in.CreateProductUseCase;
import io.nexus.catalog.application.port.in.GetProductByIdUseCase;
import io.nexus.catalog.application.port.in.ListActiveProductsUseCase;
import io.nexus.catalog.application.port.out.ProductRepository;
import io.nexus.catalog.application.usecase.CreateProductService;
import io.nexus.catalog.application.usecase.GetProductByIdService;
import io.nexus.catalog.application.usecase.ListActiveProductsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class CatalogUseCaseConfiguration {

  @Produces
  @ApplicationScoped
  CreateProductUseCase createProductUseCase(ProductRepository repository) {
    return new CreateProductService(repository);
  }

  @Produces
  @ApplicationScoped
  GetProductByIdUseCase getProductByIdUseCase(ProductRepository repository) {
    return new GetProductByIdService(repository);
  }

  @Produces
  @ApplicationScoped
  ListActiveProductsUseCase listActiveProductsUseCase(ProductRepository repository) {
    return new ListActiveProductsService(repository);
  }
}
