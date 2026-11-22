package io.nexus.catalog.infrastructure.adapter.in.rest;

import jakarta.ws.rs.core.Response.Status;
import java.util.List;

/** An HTTP error with a public message and optional client-facing details. */
public final class ApiException extends RuntimeException {

  private final Status status;
  private final String code;
  private final List<String> details;

  public ApiException(Status status, String code, String message) {
    this(status, code, message, List.of(), null);
  }

  public ApiException(Status status, String code, String message, List<String> details,
      Throwable cause) {
    super(message, cause);
    this.status = status;
    this.code = code;
    this.details = List.copyOf(details);
  }

  public Status status() {
    return status;
  }

  public String code() {
    return code;
  }

  public List<String> details() {
    return details;
  }
}
