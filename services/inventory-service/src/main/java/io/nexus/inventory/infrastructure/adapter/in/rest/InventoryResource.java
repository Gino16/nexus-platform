package io.nexus.inventory.infrastructure.adapter.in.rest;

import io.nexus.inventory.application.InventoryService;
import io.nexus.inventory.domain.InventoryException;
import io.nexus.inventory.domain.Reservation;
import io.nexus.inventory.domain.Stock;
import io.nexus.inventory.infrastructure.adapter.in.rest.generated.api.InventoryApi;
import io.nexus.inventory.infrastructure.adapter.in.rest.generated.model.AdjustStockRequest;
import io.nexus.inventory.infrastructure.adapter.in.rest.generated.model.RegisterStockRequest;
import io.nexus.inventory.infrastructure.adapter.in.rest.generated.model.ReservationLine;
import io.nexus.inventory.infrastructure.adapter.in.rest.generated.model.ReservationResponse;
import io.nexus.inventory.infrastructure.adapter.in.rest.generated.model.ReserveStockRequest;
import io.nexus.inventory.infrastructure.adapter.in.rest.generated.model.StockResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestResponse;

@RequestScoped
public class InventoryResource implements InventoryApi {

  private final InventoryService inventory;

  @Inject
  public InventoryResource(InventoryService inventory) {
    this.inventory = inventory;
  }

  @Override
  public RestResponse<StockResponse> registerStock(RegisterStockRequest request) {
    return RestResponse.status(RestResponse.Status.CREATED,
        toResponse(inventory.register(request.getProductId(), request.getQuantity())));
  }

  @Override
  public RestResponse<StockResponse> getStock(UUID productId) {
    return RestResponse.ok(toResponse(inventory.getStock(productId)));
  }

  @Override
  public RestResponse<StockResponse> adjustStock(UUID productId, AdjustStockRequest request) {
    return RestResponse.ok(toResponse(inventory.adjust(productId, request.getDelta())));
  }

  @Override
  public RestResponse<ReservationResponse> reserveStock(UUID orderId, ReserveStockRequest request) {
    var lines = request.getLines().stream().map(InventoryResource::toLine).toList();
    return RestResponse.ok(toResponse(inventory.reserve(orderId, lines)));
  }

  @Override
  public RestResponse<ReservationResponse> getReservation(UUID orderId) {
    return RestResponse.ok(toResponse(inventory.getReservation(orderId)));
  }

  @Override
  public RestResponse<ReservationResponse> releaseReservation(UUID orderId) {
    return RestResponse.ok(toResponse(inventory.release(orderId)));
  }

  @Override
  public RestResponse<ReservationResponse> confirmReservation(UUID orderId) {
    return RestResponse.ok(toResponse(inventory.confirm(orderId)));
  }

  private static Reservation.Line toLine(ReservationLine line) {
    if (line == null || line.getQuantity() == null) {
      throw new InventoryException(InventoryException.Reason.INVALID_REQUEST,
          "Each line requires a product ID and a positive quantity");
    }
    return new Reservation.Line(line.getProductId(), line.getQuantity());
  }

  private static StockResponse toResponse(Stock stock) {
    return new StockResponse(stock.productId(), stock.available(), stock.reserved());
  }

  private static ReservationResponse toResponse(Reservation reservation) {
    var lines = reservation.lines().stream()
        .map(line -> new ReservationLine(line.productId(), line.quantity()))
        .toList();
    return new ReservationResponse(reservation.orderId(),
        ReservationResponse.StatusEnum.valueOf(reservation.status().name()), lines);
  }
}
