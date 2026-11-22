package io.nexus.catalog.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.nexus.catalog.infrastructure.adapter.in.rest.generated.model.ErrorResponse;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

public class ApiExceptionMapper {

  private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class);

  @ServerExceptionMapper
  Response map(ApiException exception) {
    ErrorResponse error = new ErrorResponse(exception.code(), exception.getMessage());
    error.setDetails(exception.details());
    return Response.status(exception.status())
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(error)
        .build();
  }

  @ServerExceptionMapper
  Response invalidFields(ResteasyReactiveViolationException exception, SimpleResourceInfo resource) {
    for (var violation : exception.getConstraintViolations()) {
      for (Path.Node node : violation.getPropertyPath()) {
        if (node.getKind() == ElementKind.RETURN_VALUE) {
          LOG.error("REST response validation failed", exception);
          return map(new ApiException(Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
              "An internal error occurred"));
        }
      }
    }

    List<String> details = exception.getConstraintViolations().stream()
        .map(violation -> {
          String field = "request";
          for (Path.Node node : violation.getPropertyPath()) {
            if (node.getKind() == ElementKind.PROPERTY || node.getKind() == ElementKind.PARAMETER) {
              field = node.getName();
            }
          }
          return field + ": " + violation.getMessage();
        })
        .distinct()
        .sorted()
        .toList();
    if (isProductList(resource)) {
      return invalidQuery(details, exception);
    }
    return map(new ApiException(Status.BAD_REQUEST, "INVALID_PRODUCT",
        "The product request contains invalid fields", details, exception));
  }

  @ServerExceptionMapper
  Response invalidQueryParameter(NotFoundException exception, SimpleResourceInfo resource) {
    if (isProductList(resource) && exception.getCause() instanceof NumberFormatException) {
      return invalidQuery(List.of("page, size, minPrice and maxPrice must be numeric"), exception);
    }
    return exception.getResponse();
  }

  private Response invalidQuery(List<String> details, Throwable cause) {
    return map(new ApiException(Status.BAD_REQUEST, "INVALID_PRODUCT_QUERY",
        "The product query contains invalid parameters", details, cause));
  }

  private static boolean isProductList(SimpleResourceInfo resource) {
    return resource != null && "listActiveProducts".equals(resource.getMethodName())
        && resource.getResourceClass() != null
        && ProductsResource.class.isAssignableFrom(resource.getResourceClass());
  }

  @ServerExceptionMapper({JsonParseException.class, MismatchedInputException.class})
  Response invalidJson() {
    return map(new ApiException(Status.BAD_REQUEST, "INVALID_PRODUCT",
        "The request body must be valid product JSON"));
  }
}
