# Requirements Document

## Introduction

This document specifies requirements for two Quarkus-based applications:

1. **pdfMan** — a RESTful JAX-RS microservice providing CRUD operations on PDF documents and document templates. It accepts document template IDs combined with JSON data to generate PDFs based on stored templates. Document JSON templates and IDs are stored in the PostgreSQL database via Hibernate ORM with Panache active record pattern. Generated PDF files are stored in AWS S3 or on the local filesystem in .\documents folder; document metadata is also persisted in the PostgreSQL database.

2. **pdfAdministrator** — a companion Quarkus-based JSF administrative web application that provides a UI for managing document templates and viewing document listings with full audit trail information. It consumes the pdfMan microservice APIs.

Security (OIDC/JWT RBAC) is deferred to a future release and is NOT included in the initial release.

---

## Glossary

- **pdfMan**: The Quarkus JAX-RS microservice responsible for PDF and template CRUD operations.
- **pdfAdministrator**: The companion Quarkus JSF administrative web application.
- **Document**: A generated PDF file along with its associated metadata.
- **Template**: A JSON document template stored in the Database and managed by pdfMan. Templates define the structure and content placeholders used to render PDFs when merged with a JSON_Payload.
- **Template_Body**: The JSON object representing the template definition, supplied in the request body when creating or updating a Template.
- **Metadata**: Structured data describing a Document or Template, including name, description, and audit trail fields (created_at, updated_at, created_by, updated_by). Both Document and Template entities carry a `name` (required, unique per entity type) and a `description` (optional, free-text) field.
- **Storage_Backend**: The configured storage layer for generated PDF files — either AWS S3 or the local filesystem. Templates are stored in the Database, not in the Storage_Backend.
- **Database**: The PostgreSQL database used to persist Document and Template records (including Template_Body) via Hibernate ORM with Panache.
- **Panache_Entity**: A Hibernate ORM Panache active record entity class.
- **JSON_Payload**: A JSON object supplied by the caller to be merged with a Template to produce a PDF.
- **Audit_Trail**: The set of metadata fields tracking creation and modification timestamps and actors.
- **Native_Test**: A Quarkus native executable test that re-runs the same test logic against a native binary.

---

## Requirements

### Requirement 1: Template Management — Create and Store Templates

**User Story:** As an API consumer, I want to create and store document templates, so that I can later use them to generate PDFs.

#### Acceptance Criteria

1. WHEN a POST request containing a JSON body with a template name, an optional description, and a Template_Body is received, THE pdfMan SHALL persist a Template record (including the name, description, and Template_Body) in the Database.
2. WHEN a template is successfully stored, THE pdfMan SHALL return an HTTP 201 response containing the created Template metadata (including name and description) and a system-assigned unique identifier.
3. IF a POST request is received with a missing Template_Body or missing template name, THEN THE pdfMan SHALL return an HTTP 400 response with a descriptive error message.
4. IF a template with the same name already exists, THEN THE pdfMan SHALL return an HTTP 409 response with a descriptive error message.
5. THE pdfMan SHALL record created_at and created_by fields on the Template metadata at creation time.

---

### Requirement 2: Template Management — Retrieve Templates

**User Story:** As an API consumer, I want to retrieve stored templates, so that I can inspect or reuse them.

#### Acceptance Criteria

1. WHEN a GET request for a specific template identifier is received, THE pdfMan SHALL return an HTTP 200 response containing the Template metadata and the Template_Body stored in the Database.
2. WHEN a GET request for all templates is received, THE pdfMan SHALL return an HTTP 200 response containing a list of all Template metadata records.
3. IF a GET request references a template identifier that does not exist in the Database, THEN THE pdfMan SHALL return an HTTP 404 response with a descriptive error message.

---

### Requirement 3: Template Management — Update Templates

**User Story:** As an API consumer, I want to update an existing template, so that I can correct or improve it without losing its identifier.

#### Acceptance Criteria

1. WHEN a PUT request containing a JSON body with an updated Template_Body and/or updated name and/or updated description is received for an existing template identifier, THE pdfMan SHALL update the Template record in the Database.
2. WHEN a template is successfully updated, THE pdfMan SHALL return an HTTP 200 response containing the updated Template metadata.
3. THE pdfMan SHALL record updated_at and updated_by fields on the Template metadata at update time.
4. IF a PUT request references a template identifier that does not exist, THEN THE pdfMan SHALL return an HTTP 404 response with a descriptive error message.

---

### Requirement 4: Template Management — Delete Templates

**User Story:** As an API consumer, I want to delete a template, so that I can remove obsolete templates from the system.

#### Acceptance Criteria

1. WHEN a DELETE request for an existing template identifier is received, THE pdfMan SHALL delete the Template record from the Database.
2. WHEN a template is successfully deleted, THE pdfMan SHALL return an HTTP 204 response.
3. IF a DELETE request references a template identifier that does not exist, THEN THE pdfMan SHALL return an HTTP 404 response with a descriptive error message.
4. IF a DELETE request references a template that has associated Documents, THEN THE pdfMan SHALL return an HTTP 409 response with a descriptive error message indicating the template is in use.

---

### Requirement 5: PDF Generation

**User Story:** As an API consumer, I want to generate a PDF by combining a template with JSON data, so that I can produce populated documents programmatically.

#### Acceptance Criteria

1. WHEN a POST request containing a template identifier, a document name, an optional description, and a JSON_Payload is received, THE pdfMan SHALL merge the JSON_Payload with the identified Template and render a PDF.
2. WHEN a PDF is successfully generated, THE pdfMan SHALL store the PDF file in the Storage_Backend, persist a Document metadata record (including name and description) in the Database, and return an HTTP 201 response containing the Document metadata including a system-assigned unique identifier.
3. THE pdfMan SHALL record created_at and created_by fields on the Document metadata at generation time.
4. IF the referenced template identifier does not exist, THEN THE pdfMan SHALL return an HTTP 404 response with a descriptive error message.
5. IF the JSON_Payload is malformed or missing required fields defined by the template, THEN THE pdfMan SHALL return an HTTP 422 response with a descriptive error message.

---

### Requirement 6: Document Retrieval

**User Story:** As an API consumer, I want to retrieve generated PDF documents, so that I can download or inspect them.

#### Acceptance Criteria

1. WHEN a GET request for a specific document identifier is received, THE pdfMan SHALL return an HTTP 200 response containing the PDF binary with Content-Type header set to application/pdf.
2. WHEN a GET request for all documents is received, THE pdfMan SHALL return an HTTP 200 response containing a list of all Document metadata records.
3. IF a GET request references a document identifier that does not exist in the Database, THEN THE pdfMan SHALL return an HTTP 404 response with a descriptive error message.

---

### Requirement 7: Document Modification

**User Story:** As an API consumer, I want to regenerate or replace a stored document, so that I can update its content without losing its identifier.

#### Acceptance Criteria

1. WHEN a PUT request containing a document identifier and a new JSON_Payload (and optionally an updated name and/or description) is received, THE pdfMan SHALL regenerate the PDF using the document's associated Template and the new JSON_Payload, replace the stored PDF in the Storage_Backend, and update the Document metadata (including name and description if provided) in the Database.
2. WHEN a document is successfully updated, THE pdfMan SHALL return an HTTP 200 response containing the updated Document metadata.
3. THE pdfMan SHALL record updated_at and updated_by fields on the Document metadata at update time.
4. IF a PUT request references a document identifier that does not exist, THEN THE pdfMan SHALL return an HTTP 404 response with a descriptive error message.

---

### Requirement 8: Document Deletion

**User Story:** As an API consumer, I want to delete a stored document, so that I can remove documents that are no longer needed.

#### Acceptance Criteria

1. WHEN a DELETE request for an existing document identifier is received, THE pdfMan SHALL remove the PDF file from the Storage_Backend and delete the Document metadata record from the Database.
2. WHEN a document is successfully deleted, THE pdfMan SHALL return an HTTP 204 response.
3. IF a DELETE request references a document identifier that does not exist, THEN THE pdfMan SHALL return an HTTP 404 response with a descriptive error message.

---

### Requirement 9: Storage Backend Configuration

**User Story:** As an operator, I want to configure the storage backend, so that I can choose between AWS S3 and the local filesystem based on deployment environment.

#### Acceptance Criteria

1. WHERE the AWS S3 storage profile is active, THE pdfMan SHALL store and retrieve all generated PDF files using the configured S3 bucket.
2. WHERE the local filesystem storage profile is active, THE pdfMan SHALL store and retrieve all generated PDF files using the configured local directory path.
3. THE pdfMan SHALL support switching between storage backends via application configuration without code changes.
4. IF a storage operation fails due to an infrastructure error, THEN THE pdfMan SHALL return an HTTP 503 response with a descriptive error message.

---

### Requirement 10: Database Persistence with Panache

**User Story:** As a developer, I want all metadata persisted using Hibernate ORM with Panache active record pattern, so that the codebase follows a consistent and idiomatic Quarkus data access pattern.

#### Acceptance Criteria

1. THE pdfMan SHALL define Document and Template entities as Panache_Entity classes using the active record pattern. Both entities SHALL include `name` (required) and `description` (optional) fields in addition to their type-specific fields and audit trail fields.
2. THE pdfMan SHALL use a PostgreSQL Database for all metadata persistence, including Template_Body content.
3. THE pdfMan SHALL apply database schema migrations using Flyway or Liquibase on application startup.
4. WHEN a database operation fails due to a constraint violation, THE pdfMan SHALL return an HTTP 409 response with a descriptive error message.
5. WHEN a database operation fails due to a connectivity error, THE pdfMan SHALL return an HTTP 503 response with a descriptive error message.

---

### Requirement 11: Audit Trail

**User Story:** As an auditor, I want every Document and Template record to carry a full audit trail, so that I can determine who created or modified each record and when.

#### Acceptance Criteria

1. THE pdfMan SHALL persist created_at, updated_at, created_by, and updated_by fields on every Document and Template metadata record.
2. WHEN a Document or Template record is created, THE pdfMan SHALL set created_at to the current UTC timestamp and created_by to the caller identifier.
3. WHEN a Document or Template record is updated, THE pdfMan SHALL set updated_at to the current UTC timestamp and updated_by to the caller identifier.
4. THE pdfMan SHALL include all Audit_Trail fields in every API response that returns Document or Template metadata.

---

### Requirement 12: Template and Document Parsing / Serialization

**User Story:** As a developer, I want all JSON request and response bodies to be reliably serialized and deserialized, so that data integrity is maintained across API boundaries.

#### Acceptance Criteria

1. WHEN a JSON request body is received, THE pdfMan SHALL deserialize it into the corresponding Java object without data loss.
2. THE pdfMan SHALL serialize all Java response objects into valid JSON.
3. THE Pretty_Printer SHALL format Document and Template metadata objects into human-readable JSON representations.
4. FOR ALL valid Document and Template metadata objects, serializing then deserializing then serializing SHALL produce an equivalent object (round-trip property).

---

### Requirement 13: Unit and Native Executable Tests — pdfMan Resource Endpoints

**User Story:** As a developer, I want comprehensive tests for all resource endpoint classes, so that correctness is verified in both JVM and native executable modes.

#### Acceptance Criteria

1. THE pdfMan SHALL provide a unit test class for each JAX-RS resource endpoint class covering all CRUD operations and error paths.
2. WHEN the native executable test profile is active, THE pdfMan SHALL execute the same test cases against the native binary using the Quarkus native test runner.
3. THE pdfMan SHALL achieve a test coverage of at least 80% of lines in each resource endpoint class.
4. WHEN a resource endpoint test is executed, THE pdfMan SHALL use an in-memory or test-scoped PostgreSQL instance to avoid dependency on a production database.
5. WHEN a resource endpoint test is executed, THE pdfMan SHALL use a mock or test-double Storage_Backend to avoid dependency on AWS S3 or a real filesystem path.

---

### Requirement 14: pdfAdministrator — Template Management UI

**User Story:** As an administrator, I want a web UI to manage document templates, so that I can create, view, update, and delete templates without using the raw API.

#### Acceptance Criteria

1. THE pdfAdministrator SHALL provide a JSF page listing all Templates retrieved from the pdfMan API.
2. WHEN an administrator submits a new template via the UI, THE pdfAdministrator SHALL POST a JSON body containing the template name and Template_Body to the pdfMan API and display a success or error message.
3. WHEN an administrator selects a template for editing, THE pdfAdministrator SHALL retrieve the current Template record from the pdfMan API, allow modification of the Template_Body, and PUT the updated JSON body to the pdfMan API.
4. WHEN an administrator deletes a template via the UI, THE pdfAdministrator SHALL send a DELETE request to the pdfMan API and refresh the template listing.
5. IF the pdfMan API returns an error response, THEN THE pdfAdministrator SHALL display a user-readable error message on the UI without exposing raw HTTP status codes.

---

### Requirement 15: pdfAdministrator — Document Listing and Audit Trail UI

**User Story:** As an administrator, I want to view all generated documents with their full audit trail, so that I can monitor document activity and history.

#### Acceptance Criteria

1. THE pdfAdministrator SHALL provide a JSF page listing all Documents retrieved from the pdfMan API, including all Audit_Trail fields.
2. WHEN an administrator selects a document, THE pdfAdministrator SHALL display the full Document metadata including created_at, updated_at, created_by, and updated_by.
3. WHEN an administrator requests a document download, THE pdfAdministrator SHALL retrieve the PDF binary from the pdfMan API and stream it to the browser with Content-Type application/pdf.
4. IF the pdfMan API is unreachable, THEN THE pdfAdministrator SHALL display a user-readable connectivity error message and SHALL NOT display a blank or broken page.
