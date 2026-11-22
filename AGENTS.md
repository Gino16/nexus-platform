# Project guidance for Codex

## Project direction

- NEXUS is an educational portfolio project that demonstrates a cloud-native commerce platform; it is not a real business or production commercial system. Do not add real payment processing or assume production business obligations. Read `documentation/requirements.md` and the relevant architecture ADRs before making architectural changes.
- The repository is a Maven monorepo. Services use hexagonal architecture.
- Keep domain and application code independent of Quarkus, Panache, JPA, HTTP, and database details.
- Use contract-first API development: update the OpenAPI contract first, then regenerate the API boundary and DTOs. Never hand-edit files under `target/generated-sources/openapi`.
- Keep persistence entities and mappers separate from domain models. Panache belongs in the persistence adapter.
- Prefer YAGNI and DRY; avoid adding abstractions or infrastructure before the project needs them.

## Tests

- Use JUnit 5 and Mockito for unit tests.
- Prefer `@Mock` and `@InjectMocks` over directly constructing mocked collaborators.
- Structure tests using Arrange, Act, Assert (AAA).
- Do not run or add tests unless the user asks for testing or verification.

## Build

- The root Maven Wrapper is the shared build entry point: `./mvnw` on Linux/macOS and `mvnw.cmd` on Windows.
- Catalog module: `./mvnw -pl services/catalog-service compile -DskipTests`.
