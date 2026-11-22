package io.nexus.catalog.infrastructure.adapter.in.rest;

import io.nexus.catalog.application.port.in.CreateProductUseCase;
import io.nexus.catalog.application.port.in.GetProductByIdUseCase;
import io.nexus.catalog.application.port.in.ListActiveProductsUseCase;
import io.nexus.catalog.application.port.in.ProductPage;
import io.nexus.catalog.application.port.in.ProductSearch;
import io.nexus.catalog.application.port.in.UpdateProductByIdUseCase;
import io.nexus.catalog.domain.model.Product;
import io.nexus.catalog.infrastructure.adapter.in.rest.generated.api.ProductsApi;
import io.nexus.catalog.infrastructure.adapter.in.rest.generated.model.CreateProductRequest;
import io.nexus.catalog.infrastructure.adapter.in.rest.generated.model.ProductResponse;
import io.nexus.catalog.infrastructure.adapter.in.rest.generated.model.UpdateProductRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestResponse;

@RequestScoped
public class ProductsResource implements ProductsApi {

  private static final BigDecimal MAX_PRICE = new BigDecimal("99999999999999999.99");

  private final CreateProductUseCase createProduct;
  private final GetProductByIdUseCase getProductById;
  private final ListActiveProductsUseCase listActiveProducts;
  private final UpdateProductByIdUseCase updateProductByIdUseCase;

  @Context
  UriInfo uriInfo;

  @Inject
  public ProductsResource(CreateProductUseCase createProduct, GetProductByIdUseCase getProductById,
      ListActiveProductsUseCase listActiveProducts,
      UpdateProductByIdUseCase updateProductByIdUseCase) {
    this.createProduct = createProduct;
    this.getProductById = getProductById;
    this.listActiveProducts = listActiveProducts;
    this.updateProductByIdUseCase = updateProductByIdUseCase;
  }

  @Override
  public RestResponse<ProductResponse> createProduct(CreateProductRequest request) {
    validatePriceLimit(request.getPrice());

    Product product;
    try {
      product = createProduct.createProduct(request.getName(), request.getDescription(),
          request.getPrice());
    } catch (IllegalArgumentException exception) {
      throw invalidProduct(exception.getMessage(), exception);
    }

    URI location = uriInfo.getAbsolutePathBuilder().path(product.id().toString()).build();
    return RestResponse.ResponseBuilder.<ProductResponse>create(RestResponse.Status.CREATED)
        .entity(toResponse(product))
        .location(location)
        .build();
  }

  @Override
  public RestResponse<ProductResponse> getProductById(UUID productId) {
    Product product = getProductById.getProductById(productId)
        .filter(Product::active)
        .orElseThrow(() -> new ApiException(Status.NOT_FOUND, "PRODUCT_NOT_FOUND",
            "Active product not found"));
    return RestResponse.ok(toResponse(product));
  }

  @Override
  public RestResponse<List<ProductResponse>> listActiveProducts(Integer page, Integer size,
      String name, BigDecimal minPrice, BigDecimal maxPrice) {
    ProductSearch search;
    try {
      search = new ProductSearch(page, size, name, minPrice, maxPrice);
    } catch (IllegalArgumentException exception) {
      throw new ApiException(Status.BAD_REQUEST, "INVALID_PRODUCT_QUERY",
          "The product query contains invalid parameters", List.of(exception.getMessage()), exception);
    }

    ProductPage result = listActiveProducts.listActiveProducts(search);
    List<ProductResponse> products = result.products().stream()
        .map(ProductsResource::toResponse)
        .toList();
    return RestResponse.ResponseBuilder.ok(products)
        .header("X-Has-Next", Boolean.toString(result.hasNext()))
        .build();
  }

  @Override
  public RestResponse<ProductResponse> updateProduct(UUID productId, UpdateProductRequest request) {
    validatePriceLimit(request.getPrice());

    Product updatedProduct;
    try {
      updatedProduct = updateProductByIdUseCase.updateProductById(productId, request.getName(),
          request.getDescription(), request.getPrice(), request.getActive())
          .orElseThrow(() -> new ApiException(Status.NOT_FOUND, "PRODUCT_NOT_FOUND",
              "Product not found"));
    } catch (IllegalArgumentException exception) {
      throw invalidProduct(exception.getMessage(), exception);
    }
    return RestResponse.ok(toResponse(updatedProduct));
  }

  private static void validatePriceLimit(BigDecimal price) {
    if (price.compareTo(MAX_PRICE) > 0) {
      throw invalidProduct("price: must not exceed " + MAX_PRICE, null);
    }
  }

  private static ApiException invalidProduct(String detail, Throwable cause) {
    return new ApiException(Status.BAD_REQUEST, "INVALID_PRODUCT",
        "The product request contains invalid fields", List.of(detail), cause);
  }

  private static ProductResponse toResponse(Product product) {
    return new ProductResponse(product.id(), product.name(), product.description(), product.price(),
        product.active());
  }
}
