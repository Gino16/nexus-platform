package io.nexus.payment.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexus.payment.application.AuthorizePayment;
import io.nexus.payment.domain.Authorization;
import io.nexus.payment.domain.Authorization.Outcome;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PaymentRuntime {
  private static final Logger LOG = Logger.getLogger(PaymentRuntime.class);

  private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
  private final AuthorizationStore store;
  private final AuthorizePayment authorizePayment;
  private final MutinyEmitter<String> results;
  private final ObjectMapper json;

  @Inject
  public PaymentRuntime(AuthorizationStore store, @Channel("payment-results-out") MutinyEmitter<String> results,
      ObjectMapper json) {
    this.store = store;
    this.authorizePayment = new AuthorizePayment(store);
    this.results = results;
    this.json = json;
  }

  @Incoming("payment-requests-in")
  @Blocking
  public void receive(String payload) throws JsonProcessingException {
    var request = json.readValue(payload, PaymentRequest.class);
    if (request.schemaVersion() != 1) {
      throw new IllegalArgumentException("Unsupported payment request schema version");
    }
    authorizePayment.authorize(request.orderId(), request.amount(), request.currency(),
        request.requestedOutcome());
  }

  void start(@Observes StartupEvent event) {
    worker.scheduleWithFixedDelay(this::publishPending, 0, 1, TimeUnit.SECONDS);
  }

  void stop(@Observes ShutdownEvent event) {
    worker.shutdownNow();
  }

  private void publishPending() {
    try {
      for (int sent = 0; sent < 20; sent++) {
        if (!store.publishNext(this::publish)) {
          return;
        }
      }
    } catch (RuntimeException exception) {
      LOG.warn("Payment result publication failed and will be retried", exception);
    }
  }

  private void publish(Authorization authorization) {
    var result = new PaymentResult(authorization.orderId(), authorization.amount(),
        authorization.currency(), authorization.outcome(), 1);
    try {
      String payload = json.writeValueAsString(result);
      results.sendMessage(KafkaRecord.of(result.orderId().toString(), payload))
          .await().atMost(Duration.ofSeconds(8));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize payment result", exception);
    }
  }

  public record PaymentRequest(UUID orderId, BigDecimal amount, String currency,
      Outcome requestedOutcome, int schemaVersion) {}

  public record PaymentResult(UUID orderId, BigDecimal amount, String currency,
      Outcome outcome, int schemaVersion) {}
}
