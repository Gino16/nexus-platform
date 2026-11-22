package io.nexus.inventory.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.nexus.inventory.domain.InventoryException;
import io.nexus.inventory.infrastructure.adapter.in.rest.generated.model.ErrorResponse;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.validation.ElementKind;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import java.util.stream.StreamSupport;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class InventoryExceptionMapper {

  private static final Logger LOG = Logger.getLogger(InventoryExceptionMapper.class);

  @ServerExceptionMapper
  Response map(InventoryException exception) {
    Status status = switch (exception.reason()) {
      case INVALID_REQUEST -> Status.BAD_REQUEST;
      case STOCK_NOT_FOUND, RESERVATION_NOT_FOUND -> Status.NOT_FOUND;
      case STOCK_ALREADY_EXISTS, INSUFFICIENT_STOCK, STOCK_LIMIT_EXCEEDED, RESERVATION_CONFLICT ->
          Status.CONFLICT;
    };
    return response(status, exception.reason().name(), exception.getMessage(), List.of());
  }

  @ServerExceptionMapper
  Response invalidFields(ResteasyReactiveViolationException exception) {
    var violations = exception.getConstraintViolations();
    boolean invalidResponse = violations.stream().anyMatch(violation ->
        StreamSupport.stream(violation.getPropertyPath().spliterator(), false)
            .anyMatch(node -> node.getKind() == ElementKind.RETURN_VALUE));
    if (invalidResponse) {
      LOG.error("Inventory response validation failed", exception);
      return response(Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An internal error occurred",
          List.of());
    }

    List<String> details = violations.stream()
        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
        .distinct().sorted().toList();
    return response(Status.BAD_REQUEST, "INVALID_REQUEST", "The inventory request is invalid",
        details);
  }

  @ServerExceptionMapper({JsonParseException.class, MismatchedInputException.class})
  Response invalidJson() {
    return response(Status.BAD_REQUEST, "INVALID_REQUEST", "The request body must be valid JSON",
        List.of());
  }

  @ServerExceptionMapper
  Response invalidIdentifier(NotFoundException exception) {
    if (exception.getCause() instanceof IllegalArgumentException) {
      return response(Status.BAD_REQUEST, "INVALID_REQUEST", "The identifier must be a valid UUID",
          List.of());
    }
    return exception.getResponse();
  }

  private static Response response(Status status, String code, String message,
      List<String> details) {
    var error = new ErrorResponse(code, message);
    error.setDetails(details);
    return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(error).build();
  }
}
