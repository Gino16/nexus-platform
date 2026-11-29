package io.nexus.order.infrastructure;

import io.nexus.order.application.OrderService;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderRuntime {
  private static final Logger LOG = Logger.getLogger(OrderRuntime.class);
  private final ScheduledExecutorService worker = Executors.newScheduledThreadPool(2);

  @Produces
  @ApplicationScoped
  OrderService service(OrderPanacheStore store, CommerceHttpClient commerce) {
    return new OrderService(store, commerce, commerce);
  }

  void start(@Observes StartupEvent event, OrderPanacheStore store, OrderService service,
      KafkaOrderEvents events) {
    worker.scheduleWithFixedDelay(() -> process(store, service), 0, 1, TimeUnit.SECONDS);
    worker.scheduleWithFixedDelay(() -> publish(store, events), 0, 1, TimeUnit.SECONDS);
  }

  void stop(@Observes ShutdownEvent event) {
    worker.shutdownNow();
  }

  private void process(OrderPanacheStore store, OrderService service) {
    try {
      for (var id : store.pending()) {
        try {
          service.process(id);
        } catch (RuntimeException exception) {
          LOG.warnf(exception, "Order %s will be retried", id);
          store.defer(id);
        }
      }
    } catch (RuntimeException exception) {
      LOG.error("Order processing cycle failed", exception);
    }
  }

  private void publish(OrderPanacheStore store, KafkaOrderEvents events) {
    try {
      for (int sent = 0; sent < 20; sent++) {
        if (!store.publishNext(events)) {
          return;
        }
      }
    } catch (RuntimeException exception) {
      LOG.warn("Kafka publication failed; the outbox event will be retried", exception);
    }
  }
}
