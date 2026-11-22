# NEXUS project context

Use this document to resume work in a new Codex chat or on another development machine. The repository and its current files remain the source of truth.

## Product goal

NEXUS is an educational portfolio project demonstrating a cloud-native commerce platform through an end-to-end API-based MVP. It is not a real business, commercial product, or production system. Payments are simulated only; no real monetary transactions are in scope. The planned capabilities include catalog, inventory, orders, identity, event-driven processing, and notifications. See `requirements.md` for the baseline scope.

## Architecture and working agreements

- Maven monorepo; Java 21 and Quarkus 3.40.1.2.
- Hexagonal architecture. Keep business/domain types framework-independent.
- Contract-first HTTP APIs: OpenAPI is the source of truth; generated code is build output and must not be edited directly.
- Catalog and inventory own separate PostgreSQL databases and use Panache behind application persistence ports.
- Unit tests: JUnit 5, Mockito `@Mock` / `@InjectMocks`, AAA. Prefer YAGNI and DRY.
- Root `AGENTS.md` carries the instructions Codex should follow when opening this repository.

## Current implementation

### Catalog service

- Application use cases and ports support creating and updating products, looking up active products by ID, and listing active products with filters and pagination.
- PostgreSQL persistence is implemented with a Panache repository, persistence entity, and mapper.
- Flyway manages the schema migration.
- `devops/compose.yaml` provisions local PostgreSQL.
- `src/main/openapi/catalog-api.yaml` defines `POST /products`, `GET /products`, and `GET` and `PUT` on `/products/{productId}` under `/api/v1`.
- Maven uses OpenAPI Generator to generate a JAX-RS API interface and request/response DTOs into `target/generated-sources/openapi`; Build Helper adds those files to compilation.
- Root Maven Wrapper files (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`) let the project build without a separately installed Maven.

`ProductsResource` implements the generated `ProductsApi` and maps between the generated DTOs and the catalog use cases. `CatalogUseCaseConfiguration` wires the use cases to the persistence port through CDI producers. The REST adapter exposes only active products on reads and uses `ApiExceptionMapper` to return the OpenAPI `ErrorResponse` for catalog errors.

### Inventory service

`services/inventory-service` registers, reads, and adjusts stock. It can reserve all lines of an order
atomically and release or confirm the reservation. Reservations are keyed by order ID so retries do
not change stock twice. The service runs locally on port 8081 and uses its own PostgreSQL database
on port 5433. Order, payment, and event integration are pending.
See the [inventory service README](../services/inventory-service/README.md).

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

Open this repository in Codex and ask it to read `AGENTS.md`, `documentation/PROJECT_CONTEXT.md`, and the relevant ADRs. The generated catalog contract is already wired to the existing use cases through `ProductsResource`. Preserve the existing application and persistence boundaries when extending the catalog.
