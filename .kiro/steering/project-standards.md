# Project Standards

## Platform & Language

- Java 21 (LTS) — use modern language features (records, sealed classes, pattern matching) where appropriate
- Quarkus 3.34 — follow Quarkus idioms for CDI, configuration, and extensions
- Maven for builds; both projects share the same Quarkus BOM version

## Data Access

- Use Hibernate ORM Panache **active record pattern** — entities extend `PanacheEntity` or `PanacheEntityBase`
- Do NOT use the repository pattern (`PanacheRepository`)
- Finder methods belong on the entity class itself (e.g., `TemplateEntity.findByTemplateType(...)`)
- Keep service classes thin — they orchestrate, entities own their queries

## Project Structure

This workspace contains two Quarkus applications:

- **pdfMan** (port 8080) — JAX-RS REST microservice for template and document CRUD, PDF generation
- **pdfAdministrator** (port 8081) — JSF + PrimeFaces admin UI consuming pdfMan via MicroProfile REST Client

## Conventions

- Entities use public fields (Panache style), not private + getters/setters
- DTOs are Java records in pdfMan, mutable POJOs with getters/setters in pdfAdministrator (JSF binding requirement)
- Audit fields (`createdAt`, `createdBy`, `updatedAt`, `updatedBy`) present on all persisted entities
- `created_by`/`updated_by` are set to `"system"` until OIDC is introduced
- REST endpoints use multipart form data (`@RestForm`) for create/update operations
- Storage is abstracted behind `StorageBackend` interface — filesystem by default, S3 stub available
- Flyway manages schema migrations (`src/main/resources/db/migration/`)
- Tests use jqwik property-based testing + Testcontainers PostgreSQL in pdfMan, Mockito in pdfAdministrator
