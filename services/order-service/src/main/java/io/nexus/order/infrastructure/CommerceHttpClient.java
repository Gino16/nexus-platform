package io.nexus.order.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexus.order.application.CommercePorts;
import io.nexus.order.application.OrderFailure;
import io.nexus.order.application.OrderFailure.Reason;
import io.nexus.order.domain.PurchaseOrder.Item;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CommerceHttpClient implements CommercePorts.Catalog, CommercePorts.Inventory {
  private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  private final ObjectMapper json;
  private final String catalogUrl;
  private final String inventoryUrl;

  @Inject
  public CommerceHttpClient(ObjectMapper json, @ConfigProperty(name = "nexus.catalog-url") String catalogUrl,
      @ConfigProperty(name = "nexus.inventory-url") String inventoryUrl) {
    this.json = json;
    this.catalogUrl = catalogUrl;
    this.inventoryUrl = inventoryUrl;
  }

  @Override
  public BigDecimal activePrice(UUID productId) {
    var response = send(catalogUrl + "/products/" + productId, "GET", null);
    if (response.statusCode() == 404) {
      throw new OrderFailure(Reason.INVALID_ORDER, "Active product not found: " + productId);
    }
    requireOk(response);
    JsonNode price = body(response).path("price");
    if (!price.isNumber() || price.decimalValue().signum() < 0) {
      throw new OrderFailure(Reason.DEPENDENCY_UNAVAILABLE, "Catalog returned an invalid product price");
    }
    try {
      return new BigDecimal(price.asText()).setScale(2);
    } catch (ArithmeticException exception) {
      throw new OrderFailure(Reason.DEPENDENCY_UNAVAILABLE, "Catalog returned an invalid product price", exception);
    }
  }

  @Override
  public boolean reserve(UUID orderId, List<Item> items) {
    var response = send(inventoryUrl + "/reservations/" + orderId, "PUT", Map.of("lines", items));
    if (response.statusCode() == 404 || response.statusCode() == 409) {
      String code = body(response).path("code").asText();
      if ("STOCK_NOT_FOUND".equals(code) || "INSUFFICIENT_STOCK".equals(code)) {
        return false;
      }
    }
    requireOk(response);
    return true;
  }

  @Override
  public void settle(UUID orderId, boolean approved) {
    requireOk(send(inventoryUrl + "/reservations/" + orderId + (approved ? "/confirm" : "/release"),
        "PUT", null));
  }

  private HttpResponse<String> send(String url, String method, Object payload) {
    try {
      var builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5))
          .header("Accept", "application/json");
      var content = HttpRequest.BodyPublishers.noBody();
      if (payload != null) {
        builder.header("Content-Type", "application/json");
        content = HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload));
      }
      return client.send(builder.method(method, content).build(), HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new OrderFailure(Reason.DEPENDENCY_UNAVAILABLE, "Commerce request interrupted", exception);
    } catch (IOException exception) {
      throw new OrderFailure(Reason.DEPENDENCY_UNAVAILABLE, "Commerce service unavailable", exception);
    }
  }

  private JsonNode body(HttpResponse<String> response) {
    try {
      return json.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
          .readTree(response.body());
    } catch (IOException exception) {
      throw new OrderFailure(Reason.DEPENDENCY_UNAVAILABLE, "Invalid commerce response", exception);
    }
  }

  private static void requireOk(HttpResponse<String> response) {
    if (response.statusCode() != 200) {
      throw new OrderFailure(Reason.DEPENDENCY_UNAVAILABLE,
          "Unexpected response from commerce service: " + response.statusCode());
    }
  }
}
