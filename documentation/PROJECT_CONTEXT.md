# NEXUS project context

Use this document to resume work in a new Codex chat or on another development machine. The repository and its current files remain the source of truth.

## Product goal

NEXUS is an educational portfolio project demonstrating a cloud-native commerce platform through an end-to-end API-based MVP. It is not a real business, commercial product, or production system. Payments are simulated only; no real monetary transactions are in scope. The planned capabilities include catalog, inventory, orders, identity, event-driven processing, and notifications. See `requirements.md` for the baseline scope.

## Architecture and working agreements

- Maven monorepo; Java 21 and Quarkus 3.40.1.2.
- Hexagonal architecture. Keep business/domain types framework-independent.
- Contract-first HTTP APIs: OpenAPI is the source of truth; generated code is build output and must not be edited directly.
- PostgreSQL persistence uses a Panache repository behind the application persistence port. JPA entities and mapping remain in infrastructure.
- Unit tests: JUnit 5, Mockito `@Mock` / `@InjectMocks`, AAA. Prefer YAGNI and DRY.
- Root `AGENTS.md` carries the instructions Codex should follow when opening this repository.

## Current implementation

The active service is `services/catalog-service`:

- Application use cases and ports support creating a product, looking up a product by ID, and listing active products.
- PostgreSQL persistence is implemented with a Panache repository, persistence entity, and mapper.
- Flyway manages the schema migration.
- `devops/compose.yaml` provisions local PostgreSQL.
- `src/main/openapi/catalog-api.yaml` defines `POST /products`, `GET /products`, and `GET /products/{productId}` under `/api/v1`.
- Maven uses OpenAPI Generator to generate a JAX-RS API interface and request/response DTOs into `target/generated-sources/openapi`; Build Helper adds those files to compilation.
- Root Maven Wrapper files (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`) let the project build without a separately installed Maven.

The generated `ProductsApi` is only the HTTP boundary. A handwritten REST adapter that implements it and maps between generated DTOs and application use cases is the next implementation step. Error-to-HTTP mapping should also be handled at that boundary.

## Useful commands

From the repository root on Linux:

```bash
./mvnw -pl services/catalog-service compile -DskipTests
cp devops/.env.example .env
docker compose -f devops/compose.yaml up -d
./mvnw -pl services/catalog-service quarkus:dev
```

The first command generates the OpenAPI Java sources and compiles them. Do not commit generated `target` output. No test suite was run during the OpenAPI generation/Wrapper setup step.

## Resume prompt

Open this repository in Codex and ask it to read `AGENTS.md`, `documentation/PROJECT_CONTEXT.md`, and the relevant ADRs. Continue by implementing the REST adapter for the generated catalog contract, preserving the existing application and persistence boundaries.
