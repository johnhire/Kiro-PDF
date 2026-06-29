# Implementation Plan: pdf-man-service

## Overview

Two Quarkus projects are implemented in sequence: **pdfMan** (JAX-RS microservice) first, then **pdfAdministrator** (JSF web app). Within pdfMan, the order is: project scaffold → database layer → storage layer → service layer → REST resources → exception mappers → tests. pdfAdministrator follows once pdfMan's API contract is stable.

## Tasks

- [x] 1. Scaffold pdfMan Quarkus project
  - [x] 1.1 Create `pdfMan/pom.xml` with Quarkus BOM and required extensions
    - Extensions: `quarkus-resteasy-reactive-jackson`, `quarkus-hibernate-orm-panache`, `quarkus-jdbc-postgresql`, `quarkus-flyway`, `quarkus-amazon-s3`, `quarkus-smallrye-openapi`
    - Add OpenPDF and Thymeleaf dependencies (non-Quarkus extension JARs)
    - Add jqwik, REST-assured, and Testcontainers test dependencies
    - _Requirements: 10.1, 10.2, 10.3, 13.1_
  - [x] 1.2 Create `pdfMan/src/main/resources/application.properties`
    - Configure datasource (PostgreSQL), Flyway, and storage backend profile properties
    - Define `%test` profile overrides for Testcontainers and mock storage
    - _Requirements: 9.1, 9.2, 9.3, 10.2_
  - [x] 1.3 Create `pdfMan/src/main/resources/db/migration/V1__init.sql`
    - Write Flyway migration creating `template` and `document` tables per the design schema
    - _Requirements: 10.3_

- [x] 2. Implement core data layer — entities and DTOs
  - [x] 2.1 Create `TemplateEntity` Panache active record class
    - Extend `PanacheEntity`, map all columns, implement `findByName` finder
    - _Requirements: 10.1, 11.1_
  - [x] 2.2 Create `DocumentEntity` Panache active record class
    - Extend `PanacheEntity`, map all columns including `@ManyToOne` to `TemplateEntity`
    - _Requirements: 10.1, 11.1_
  - [x] 2.3 Create DTO records: `TemplateDto`, `DocumentDto`, `GeneratePdfRequest`
    - Java records with all fields matching the design specification
    - _Requirements: 12.1, 12.2_
  - [x] 2.4 Write property test for DTO serialization round-trip (Property 16)
    - **Property 16: DTO Serialization Round-Trip**
    - Use jqwik `@Property(tries = 100)` with arbitrary generators for `TemplateDto` and `DocumentDto`
    - Serialize to JSON and deserialize back; assert all fields equal the original
    - **Validates: Requirements 12.1, 12.2, 12.4**

- [x] 3. Implement StorageBackend abstraction and implementations
  - [x] 3.1 Create `StorageBackend` CDI interface with `store`, `retrieve`, and `delete` methods
    - _Requirements: 9.1, 9.2, 9.3_
  - [x] 3.2 Create `FilesystemStorageBackend` implementing `StorageBackend`
    - Read/write files under a configurable base directory; inject path from config
    - _Requirements: 9.2, 9.3_
  - [x] 3.3 Create `S3StorageBackend` implementing `StorageBackend`
    - Delegate to Quarkus S3 client; use configured bucket name from application properties
    - _Requirements: 9.1, 9.3_
  - [x] 3.4 Create `MockStorageBackend` test `@Alternative` CDI bean
    - Backed by `ConcurrentHashMap<String, byte[]>`; activated in `%test` profile
    - Place in `src/test/java/`
    - _Requirements: 13.5_

- [x] 4. Implement AuditHelper and exception hierarchy
  - [x] 4.1 Create `AuditHelper` utility class
    - Methods `stampCreated(entity)` and `stampUpdated(entity)` setting `createdAt`/`updatedAt` to `OffsetDateTime.now()` and `*_by` to `"system"`
    - _Requirements: 11.1, 11.2, 11.3_
  - [x] 4.2 Create custom exception classes
    - `NotFoundException`, `DuplicateNameException`, `TemplateInUseException`, `InvalidPayloadException`, `StorageException`, `DatabaseConnectivityException`
    - _Requirements: 9.4, 10.4, 10.5_
  - [x] 4.3 Create JAX-RS `ExceptionMapper` classes for each exception type
    - Map each exception to the correct HTTP status and `{"error": "..."}` JSON body per the design error table
    - _Requirements: 9.4, 10.4, 10.5_

- [x] 5. Implement PdfRenderingService
  - [x] 5.1 Create `PdfRenderingService` bean
    - Accept a template content string and `Map<String, Object>` payload
    - Use Thymeleaf `TemplateEngine` to render HTML from the template string and payload
    - Use OpenPDF `HtmlConverter` (or `Document`/`PdfWriter`) to convert rendered HTML to a `byte[]`
    - Throw `InvalidPayloadException` if Thymeleaf rendering fails due to missing variables
    - _Requirements: 5.1, 5.5_

- [x] 6. Implement TemplateService and TemplateResource
  - [x] 6.1 Create `TemplateService` CDI bean
    - `create(name, content)`: check duplicate via `findByName`, stamp audit, persist, store content via `StorageBackend`, return `TemplateDto`
    - `findById(id)`: return `TemplateDto` or throw `NotFoundException`
    - `listAll()`: return `List<TemplateDto>`
    - `update(id, content)`: find or throw, stamp audit update, persist, return `TemplateDto`
    - `delete(id)`: find or throw, check for associated documents (throw `TemplateInUseException` if any), delete from storage and DB
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4_
  - [x] 6.2 Create `TemplateResource` JAX-RS class at `/api/templates`
    - Implement `POST`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` delegating to `TemplateService`
    - Multipart form handling for POST and PUT (template name + content fields)
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 4.1, 4.2_
  - [x] 6.3 Write property test for template creation round-trip (Property 1)
    - **Property 1: Template Creation Round-Trip**
    - `@Property(tries = 100)` — generate random valid name + content, POST, assert 201 + non-null id, GET by id, assert same name and content
    - **Validates: Requirements 1.1, 1.2**
  - [x] 6.4 Write property test for missing fields returns 400 (Property 2)
    - **Property 2: Template Creation with Missing Fields Returns 400**
    - `@Property(tries = 100)` — POST with missing name or missing content, assert 400 + non-empty error message
    - **Validates: Requirements 1.3**
  - [x] 6.5 Write property test for duplicate template name returns 409 (Property 3)
    - **Property 3: Duplicate Template Name Returns 409**
    - `@Property(tries = 100)` — POST same name twice, assert second returns 409
    - **Validates: Requirements 1.4**
  - [x] 6.6 Write property test for template list completeness (Property 5)
    - **Property 5: Template List Contains All Created Templates**
    - `@Property(tries = 100)` — create N templates, GET list, assert all ids present
    - **Validates: Requirements 2.1, 2.2**
  - [x] 6.7 Write property test for non-existent template returns 404 (Property 6 — templates)
    - **Property 6: Non-Existent Resource Returns 404**
    - `@Property(tries = 100)` — GET/PUT/DELETE with random large id not in DB, assert 404
    - **Validates: Requirements 2.3, 3.4, 4.3**
  - [x] 6.8 Write property test for template update round-trip (Property 7)
    - **Property 7: Template Update Round-Trip**
    - `@Property(tries = 100)` — create template, PUT new content, assert 200 + new content, GET and assert new content
    - **Validates: Requirements 3.1, 3.2, 3.3**
  - [x] 6.9 Write property test for template delete round-trip (Property 8)
    - **Propmerty 8: Template Delete Round-Trip**
    - `@Property(tries = 100)` — create template, DELETE, assert 204, GET assert 404
    - **Validates: Requirements 4.1, 4.2**
  - [x] 6.10 Write property test for template in-use delete returns 409 (Property 9)
    - **Property 9: Template In-Use Delete Returns 409**
    - `@Property(tries = 100)` — create template, generate a document from it, DELETE template, assert 409
    - **Validates: Requirements 4.4**

- [x] 7. Implement DocumentService and DocumentResource
  - [x] 7.1 Create `DocumentService` CDI bean
    - `generate(templateId, payload)`: resolve template or throw `NotFoundException`, invoke `PdfRenderingService`, store PDF via `StorageBackend`, stamp audit, persist `DocumentEntity`, return `DocumentDto`
    - `findById(id)`: return `DocumentEntity` or throw `NotFoundException`
    - `listAll()`: return `List<DocumentDto>`
    - `getPdfBytes(id)`: retrieve PDF bytes from `StorageBackend`
    - `update(id, payload)`: find or throw, re-render PDF, replace in storage, stamp audit update, return `DocumentDto`
    - `delete(id)`: find or throw, delete from storage and DB
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3, 7.1, 7.2, 7.3, 7.4, 8.1, 8.2, 8.3_
  - [x] 7.2 Create `DocumentResource` JAX-RS class at `/api/documents`
    - `POST /api/documents` — accepts `GeneratePdfRequest` JSON, returns 201 + `DocumentDto`
    - `GET /api/documents` — returns list of `DocumentDto`
    - `GET /api/documents/{id}` — returns PDF bytes with `Content-Type: application/pdf`
    - `PUT /api/documents/{id}` — accepts `GeneratePdfRequest` JSON, returns 200 + `DocumentDto`
    - `DELETE /api/documents/{id}` — returns 204
    - _Requirements: 5.2, 6.1, 6.2, 7.2, 8.2_
  - [x] 7.3 Write property test for PDF generation round-trip (Property 10)
    - **Property 10: PDF Generation Round-Trip**
    - `@Property(tries = 100)` — create template, POST generate, assert 201 + non-null id, GET by id, assert 200 + `Content-Type: application/pdf` + non-empty body
    - **Validates: Requirements 5.1, 5.2, 6.1**
  - [x] 7.4 Write property test for invalid payload returns 422 (Property 11)
    - **Property 11: Invalid Payload Returns 422**
    - `@Property(tries = 100)` — POST with malformed/missing payload fields, assert 422 + non-empty error message
    - **Validates: Requirements 5.5**
  - [x] 7.5 Write property test for document list completeness (Property 12)
    - **Property 12: Document List Contains All Generated Documents**
    - `@Property(tries = 100)` — generate N documents, GET list, assert all ids present
    - **Validates: Requirements 6.2**
  - [x] 7.6 Write property test for non-existent document returns 404 (Property 6 — documents)
    - **Property 6: Non-Existent Resource Returns 404 (documents)**
    - `@Property(tries = 100)` — GET/PUT/DELETE with random large id not in DB, assert 404
    - **Validates: Requirements 6.3, 7.4, 8.3**
  - [x] 7.7 Write property test for document update round-trip (Property 13)
    - **Property 13: Document Update Round-Trip**
    - `@Property(tries = 100)` — generate document, PUT new payload, assert 200 + non-null `updated_at` at or after `created_at`
    - **Validates: Requirements 7.1, 7.2, 7.3**
  - [x] 7.8 Write property test for document delete round-trip (Property 14)
    - **Property 14: Document Delete Round-Trip**
    - `@Property(tries = 100)` — generate document, DELETE, assert 204, GET assert 404
    - **Validates: Requirements 8.1, 8.2**

- [x] 8. Implement audit field and storage failure property tests
  - [x] 8.1 Write property test for audit fields on create and update (Property 4)
    - **Property 4: Audit Fields Present on All Created and Updated Entities**
    - `@Property(tries = 100)` — create template and document, assert `created_at` non-null, `created_by` non-null, `updated_at` null; then update, assert `updated_at` non-null and >= `created_at`, `updated_by` non-null
    - **Validates: Requirements 1.5, 5.3, 11.1, 11.2, 11.3, 11.4**
  - [x] 8.2 Write property test for storage backend failure returns 503 (Property 15)
    - **Property 15: Storage Backend Failure Returns 503**
    - `@Property(tries = 100)` — configure `MockStorageBackend` to throw `StorageException` on demand, trigger store/retrieve/delete, assert 503 + non-empty error message
    - **Validates: Requirements 9.4, 10.5**

- [x] 9. Checkpoint — pdfMan core complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Create native integration test stubs for pdfMan
  - [x] 10.1 Create `NativeTemplateResourceIT` extending `TemplateResourceTest`
    - Empty body annotated with `@QuarkusIntegrationTest`; re-runs all template tests against native binary
    - _Requirements: 13.2_
  - [x] 10.2 Create `NativeDocumentResourceIT` extending `DocumentResourceTest`
    - Empty body annotated with `@QuarkusIntegrationTest`; re-runs all document tests against native binary
    - _Requirements: 13.2_

- [x] 11. Set up pdfAdministrator Quarkus project
  - [x] 11.1 Populate `pdfAdministrator/pom.xml` with Quarkus BOM and required extensions
    - Extensions: `quarkus-jsf` (MyFaces), `quarkus-rest-client-reactive-jackson`, `quarkus-smallrye-fault-tolerance`
    - _Requirements: 14.1, 15.1_
  - [x] 11.2 Create `pdfAdministrator/src/main/resources/application.properties`
    - Configure `PdfManClient` base URL and any JSF settings
    - _Requirements: 14.1_

- [x] 12. Implement PdfManClient REST client interface
  - [x] 12.1 Create `PdfManClient` MicroProfile REST Client interface
    - Annotate with `@RegisterRestClient`; mirror all pdfMan endpoints: template CRUD and document CRUD
    - Return types match `TemplateDto`, `DocumentDto`, `GeneratePdfRequest` (shared or duplicated records)
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 15.1, 15.3_

- [x] 13. Implement JSF backing beans and pages
  - [x] 13.1 Create `TemplateListBean` `@Named @ViewScoped` backing bean
    - `loadTemplates()` calls `PdfManClient.listTemplates()`; `deleteTemplate(id)` calls DELETE and refreshes list
    - Wrap calls in try/catch for `WebApplicationException` and `ProcessingException`; set `FacesMessage` on error
    - _Requirements: 14.1, 14.4, 14.5_
  - [x] 13.2 Create `TemplateEditBean` `@Named @ViewScoped` backing bean
    - `loadTemplate(id)` for edit; `save()` calls POST or PUT on `PdfManClient`; error handling via `FacesMessage`
    - _Requirements: 14.2, 14.3, 14.5_
  - [x] 13.3 Create `DocumentListBean` `@Named @ViewScoped` backing bean
    - `loadDocuments()` calls `PdfManClient.listDocuments()`; exposes full audit trail fields
    - Connectivity error handling: catch `ProcessingException`, set error `FacesMessage`, do not leave blank page
    - _Requirements: 15.1, 15.2, 15.4_
  - [x] 13.4 Create `DocumentDetailBean` `@Named @ViewScoped` backing bean
    - `downloadPdf(id)` retrieves PDF bytes from `PdfManClient` and writes to `FacesContext` response with `Content-Type: application/pdf`
    - _Requirements: 15.3_
  - [x] 13.5 Create JSF XHTML pages: `templates.xhtml`, `template-edit.xhtml`, `documents.xhtml`, `document-detail.xhtml`
    - Wire each page to its backing bean; display audit trail fields in document pages; show `FacesMessage` error area on each page
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 15.1, 15.2, 15.3, 15.4_

- [x] 14. Implement pdfAdministrator backing bean unit tests
  - [x] 14.1 Create `TemplateListBeanTest` using Mockito
    - Mock `PdfManClient`; verify `listTemplates()` called on load; verify `FacesMessage` set on `WebApplicationException` and `ProcessingException`
    - _Requirements: 14.1, 14.4, 14.5_
  - [x] 14.2 Create `TemplateEditBeanTest` using Mockito
    - Verify POST called on save for new template; verify PUT called on save for existing; verify `FacesMessage` on API error
    - _Requirements: 14.2, 14.3, 14.5_
  - [x] 14.3 Create `DocumentListBeanTest` using Mockito
    - Verify `listDocuments()` called on load; verify connectivity error message set when `ProcessingException` thrown
    - _Requirements: 15.1, 15.4_
  - [x] 14.4 Create `DocumentDetailBeanTest` using Mockito
    - Verify PDF bytes written to response stream; verify `Content-Type: application/pdf` set
    - _Requirements: 15.3_

- [x] 15. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Property tests use jqwik `@Property(tries = 100)` and must include the comment `// Feature: pdf-man-service, Property N: <property text>`![alt text](image.png)
- `MockStorageBackend` is activated via `@Alternative` + `@Priority` in the `test` profile — no real S3 or filesystem access in tests
- `created_by` / `updated_by` fields are populated with `"system"` as a placeholder until OIDC is introduced
