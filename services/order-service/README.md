# Order service

The local checkout flow uses catalog on port 8080, inventory on port 8081, and orders on port 8082. Payment consumes and publishes Kafka messages.
Each service owns its own PostgreSQL database. From the repository root, start the databases:

```bash
docker compose -f devops/compose.yaml up -d --wait catalog-postgres inventory-postgres order-postgres payment-postgres kafka
```

Run each service in a separate terminal. If the wrapper lacks execute permission, invoke it with Bash.

```bash
bash ./mvnw -pl services/catalog-service quarkus:dev
bash ./mvnw -pl services/inventory-service quarkus:dev -Ddebug=5006
bash ./mvnw -pl services/order-service quarkus:dev -Ddebug=5007
bash ./mvnw -pl services/payment-service quarkus:dev -Ddebug=5008
```

Submit a request to `PUT /api/v1/orders/{orderId}` with a new UUID, a customer UUID, an outcome of
`APPROVED` or `DECLINED`, and a list of product IDs and quantities. The API returns `202` and
`GET /api/v1/orders/{orderId}` returns the current state. Reuse the same order ID and body on retry.
The corresponding products must be active in catalog and have stock registered in inventory.

Order processing retries transient upstream errors. The worker uses inventory's idempotent reserve,
confirm, and release endpoints, and stores a terminal notification event in the same transaction as
the order state. The outbox publisher sends terminal events to Kafka. A subscriber in the notification consumer group logs each event. The checkout uses HTTP for catalog and inventory, and Kafka to request and receive [simulated payments](../payment-service/README.md). Orders wait in `PROCESSING` while payment is unavailable. Keycloak authentication and customer ownership checks remain pending. The order API is bound to loopback by default for local use.

Run the checkout tests with all four services running:

```bash
python3 services/order-service/src/test/integration/test_checkout.py
```

The test uses fresh IDs and leaves test records in the local databases. Import
[the OpenAPI contract](src/main/openapi/order-api.yaml) for manual calls.


## Kafka terminal events

The local broker listens on `localhost:9092`. OrderService publishes `OrderConfirmed` and
`OrderRejected` to `nexus.orders.events.v1`, keyed by order ID. The JSON envelope contains
`eventId`, `orderId`, `type`, `occurredAt`, and `schemaVersion`. Each order produces one
terminal event, so its order ID is also the stable event ID.

The publisher marks an outbox row as published after Kafka acknowledges it. When the broker is
unreachable, the event remains pending and checkout continues. Delivery is at least once, so a
crash between Kafka acknowledgement and the database commit can cause redelivery. Existing outbox
rows already marked published by the previous local logger are not replayed.

Inspect the topic from the repository root:

```bash
docker compose -f devops/compose.yaml exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:29092 --topic nexus.orders.events.v1 --from-beginning \
  --property print.key=true --timeout-ms 5000
```
