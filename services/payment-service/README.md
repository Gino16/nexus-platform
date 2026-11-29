# Payment service

Payment consumes requests on `nexus.payments.requests.v1` and publishes results on
`nexus.payments.results.v1`. It runs independently of orders with its own PostgreSQL database
and has no HTTP endpoint. See the [Kafka contract](../../documentation/architecture/integrations/payment-events.yaml).

The service uses the requested outcome to simulate approval or rejection in PEN. It records exactly
one authorization per order ID. An identical retry reuses that record. A different amount or outcome
for the same ID is a conflict. The authorization row also serves as the outbox entry, so publication
can resume after a restart. Results use at-least-once delivery, and orders applies repeated results
idempotently.

To start the service locally:

```bash
docker compose -f devops/compose.yaml up -d --wait payment-postgres kafka
bash ./mvnw -pl services/payment-service quarkus:dev -Ddebug=5008
```

Use the [checkout runbook](../order-service/README.md) to run the complete flow. The request and
result topic names can be overridden with `PAYMENT_REQUESTS_TOPIC` and `PAYMENT_RESULTS_TOPIC`.
Configure the same values in both services.

Messages that fail processing are retried on Kafka retry topics. After exhausting retries, they
are routed to the corresponding requests or results topic with the `.dead-letter` suffix.
