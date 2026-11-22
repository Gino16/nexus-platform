# Local development infrastructure

This directory contains the Docker Compose definitions for local dependencies. The stack is
being added incrementally; PostgreSQL is the first service.

## Start PostgreSQL

From the repository root, copy the sample environment file and start the database:

```bash
cp devops/.env.example .env
docker compose -f devops/compose.yaml up -d
```

The database listens on `localhost:5432` and stores data in the named
`catalog-postgres-data` volume. Compose waits for PostgreSQL readiness before reporting the
container as healthy.

The sample credentials are for local development only. Do not reuse them in deployed
environments. The `.env` file is ignored by Git.

## Run the catalog service

After PostgreSQL is healthy, run the service in dev mode from the repository root:

```bash
./mvnw -pl services/catalog-service quarkus:dev
```

Flyway applies versioned migrations from `src/main/resources/db/migration` at startup.
