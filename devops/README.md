# Local development infrastructure

This directory contains the Docker Compose definitions for the local catalog and inventory databases.

## Start PostgreSQL

From the repository root, copy the sample environment file and start the database:

```bash
cp devops/.env.example .env
docker compose -f devops/compose.yaml up -d
```

Catalog PostgreSQL listens on `localhost:5432` and uses the `catalog-postgres-data` volume.
Inventory PostgreSQL listens on `localhost:5433` and uses the `inventory-postgres-data` volume.
Each database has its own readiness check.

The sample credentials are for local development only. Do not reuse them in deployed
environments. The `.env` file is ignored by Git.

## Run the catalog service

After PostgreSQL is healthy, run the service in dev mode from the repository root:

```bash
./mvnw -pl services/catalog-service quarkus:dev
```

Flyway applies versioned migrations from `src/main/resources/db/migration` at startup.


## Run the inventory service

From the repository root:

```bash
docker compose -f devops/compose.yaml up -d --wait inventory-postgres
./mvnw -pl services/inventory-service quarkus:dev
```

The inventory API listens on `http://localhost:8081/api/v1`. Flyway creates the inventory tables at startup.
If you change the database port, name, or credentials, set `INVENTORY_DB_URL`, `INVENTORY_DB_USERNAME`,
and `INVENTORY_DB_PASSWORD` in the environment used to start Quarkus.
See the [inventory service README](../services/inventory-service/README.md) for the reservation flow.

## Run the local checkout flow

Order PostgreSQL listens on `localhost:5434` and uses the `order-postgres-data` volume.
See the [order service README](../services/order-service/README.md) for the checkout startup
commands, order API, and end-to-end test.

Kafka runs in KRaft mode and listens on `localhost:9092` for applications started on the host.
The [order service README](../services/order-service/README.md#kafka-terminal-events) describes the
terminal events and how to inspect the topic.

Payment PostgreSQL listens on `localhost:5435` and uses the `payment-postgres-data` volume.
