package io.nexus.order.infrastructure;

import io.nexus.order.application.OrderFailure;
import io.nexus.order.application.OrderService;
import io.nexus.order.domain.PurchaseOrder;
import io.nexus.order.infrastructure.adapter.in.rest.generated.api.OrdersApi;
import io.nexus.order.infrastructure.adapter.in.rest.generated.model.OrderLineResponse;
import io.nexus.order.infrastructure.adapter.in.rest.generated.model.OrderRequest;
import io.nexus.order.infrastructure.adapter.in.rest.generated.model.OrderResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@RequestScoped
public class OrdersResource implements OrdersApi {
  private final OrderService orders;

  @Inject
  public OrdersResource(OrderService orders) {
    this.orders = orders;
  }

  @Override
  public RestResponse<OrderResponse> submitOrder(UUID orderId, OrderRequest request) {
    var lines = request.getLines().stream().map(line -> {
      if (line == null || line.getQuantity() == null) {
        throw new OrderFailure(OrderFailure.Reason.INVALID_ORDER, "Each line requires a product and quantity");
      }
      return new PurchaseOrder.Item(line.getProductId(), line.getQuantity());
    }).toList();
    var order = orders.submit(orderId, request.getCustomerId(),
        PurchaseOrder.PaymentOutcome.valueOf(request.getPaymentOutcome().name()), lines);
    return RestResponse.status(RestResponse.Status.ACCEPTED, toResponse(order));
  }

  @Override
  public RestResponse<OrderResponse> getOrder(UUID orderId) {
    return RestResponse.ok(toResponse(orders.get(orderId)));
  }

  @ServerExceptionMapper
  Response error(OrderFailure exception) {
    var status = switch (exception.reason()) {
      case INVALID_ORDER -> Response.Status.BAD_REQUEST;
      case ORDER_NOT_FOUND -> Response.Status.NOT_FOUND;
      case ORDER_CONFLICT -> Response.Status.CONFLICT;
      case DEPENDENCY_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
    };
    return Response.status(status).type("application/json")
        .entity(Map.of("code", exception.reason().name(), "message", exception.getMessage(),
            "details", List.of())).build();
  }

  private static OrderResponse toResponse(PurchaseOrder order) {
    var lines = order.lines().stream().map(line -> new OrderLineResponse(line.item().productId(),
        line.item().quantity(), line.unitPrice())).toList();
    return new OrderResponse(order.id(), order.customerId(),
        OrderResponse.StatusEnum.valueOf(order.status().name()), order.total(), order.createdAt(), lines)
        .rejectionReason(order.rejectionReason());
  }
}
