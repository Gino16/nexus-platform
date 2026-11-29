package io.nexus.order.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexus.order.domain.PurchaseOrder;
import io.nexus.order.domain.PurchaseOrder.PaymentOutcome;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KafkaOrderEvents {

  private static final Logger LOG = Logger.getLogger(KafkaOrderEvents.class);

  private final MutinyEmitter<String> emitter;
  private final MutinyEmitter<String> paymentRequests;
  private final OrderPanacheStore orders;
  private final ObjectMapper json;

  @Inject
  public KafkaOrderEvents(@Channel("order-events-out") MutinyEmitter<String> emitter,
      @Channel("payment-requests-out") MutinyEmitter<String> paymentRequests,
      OrderPanacheStore orders, ObjectMapper json) {
    this.emitter = emitter;
    this.paymentRequests = paymentRequests;
    this.orders = orders;
    this.json = json;
  }

  public void publish(OrderEvent event) {
    publish(emitter, event.orderId(), event);
  }

  public void requestPayment(PurchaseOrder order) {
    publish(paymentRequests, order.id(),
        new PaymentRequest(order.id(), order.total(), "PEN", order.paymentOutcome(), 1));
  }

  @Incoming("payment-results-in")
  @Blocking
  public void receivePayment(String payload) throws JsonProcessingException {
    var result = json.readValue(payload, PaymentResult.class);
    if (result.schemaVersion() != 1 || !"PEN".equals(result.currency()) || result.orderId() == null
        || result.amount() == null || result.outcome() == null) {
      throw new IllegalArgumentException("Invalid payment result");
    }
    orders.recordPayment(result.orderId(), result.amount(), result.outcome());
  }

  private void publish(MutinyEmitter<String> channel, UUID orderId, Object event) {
    try {
      String payload = json.writeValueAsString(event);
      channel.sendMessage(KafkaRecord.of(orderId.toString(), payload))
          .await().atMost(Duration.ofSeconds(8));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize order event", exception);
    }
  }

  @Incoming("order-notifications-in")
  public void notify(String payload) throws JsonProcessingException {
    var event = json.readValue(payload, OrderEvent.class);
    LOG.infof("Order notification %s for %s [eventId=%s]", event.type(), event.orderId(),
        event.eventId());
  }

  public record PaymentRequest(UUID orderId, BigDecimal amount, String currency,
      PaymentOutcome requestedOutcome, int schemaVersion) {}

  public record PaymentResult(UUID orderId, BigDecimal amount, String currency,
      PaymentOutcome outcome, int schemaVersion) {}
}
