package com.pdfman;

import com.pdfman.dto.DocumentDto;
import com.pdfman.dto.TemplateDto;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DocumentResource.
 * Feature: pdf-man-service
 */
@QuarkusTest
class DocumentResourceTest {

    @Inject
    MockStorageBackend mockStorageBackend;

    @BeforeEach
    void resetStorage() {
        mockStorageBackend.reset();
    }

    // -------------------------------------------------------------------------
    // Test 10: PDF Generation Round-Trip
    // -------------------------------------------------------------------------

    @Test
    void pdfGenerationRoundTrip() {
        for (int i = 0; i < 10; i++) {
            String name = UUID.randomUUID().toString().replace("-", "");
            String content = "Hello World " + i;

            TemplateDto template = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateType", name)
                    .multiPart("content", content)
                    .when()
                    .post("/api/templates")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(TemplateDto.class);

            assertNotNull(template.id(), "template id must be non-null after creation");

            DocumentDto document = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateId", template.id().toString())
                    .multiPart("name", "test-doc")
                    .multiPart("jsonOriginString", "{}")
                    .when()
                    .post("/api/documents")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(DocumentDto.class);

            assertNotNull(document.id(), "document id must be non-null after generation");

            byte[] pdfBytes = given()
                    .when()
                    .get("/api/documents/" + document.id())
                    .then()
                    .statusCode(200)
                    .contentType("application/pdf")
                    .extract()
                    .asByteArray();

            assertNotNull(pdfBytes, "PDF bytes must not be null");
            assertTrue(pdfBytes.length > 0, "PDF body must be non-empty");
        }
    }

    // -------------------------------------------------------------------------
    // Test 11: Invalid Payload Returns 422
    // -------------------------------------------------------------------------

    @Test
    void nonExistentTemplateReturns404OnGenerate() {
        for (int i = 0; i < 10; i++) {
            long fakeTemplateId = 9_999_000L + i + System.nanoTime() % 1000;

            String errorBody = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateId", String.valueOf(fakeTemplateId))
                    .multiPart("name", "test-doc")
                    .multiPart("jsonOriginString", "{}")
                    .when()
                    .post("/api/documents")
                    .then()
                    .statusCode(404)
                    .extract()
                    .asString();

            assertNotNull(errorBody, "error body must not be null for non-existent template");
            assertFalse(errorBody.isBlank(), "error message must be non-empty for non-existent template");
        }
    }

    // -------------------------------------------------------------------------
    // Test 12: Document List Contains All Generated Documents
    // -------------------------------------------------------------------------

    @Test
    void documentListContainsAllGeneratedDocuments() {
        for (int i = 0; i < 10; i++) {
            List<String[]> documents = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                documents.add(new String[]{
                        UUID.randomUUID().toString().replace("-", ""),
                        "Hello World " + i + "_" + j
                });
            }

            String templateName = documents.get(0)[0];
            String templateContent = documents.get(0)[1];

            TemplateDto template = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateType", templateName)
                    .multiPart("content", templateContent)
                    .when()
                    .post("/api/templates")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(TemplateDto.class);

            assertNotNull(template.id(), "template id must be non-null after creation");

            List<Long> createdDocumentIds = new ArrayList<>();
            for (String[] ignored : documents) {
                DocumentDto doc = given()
                        .contentType(ContentType.MULTIPART)
                        .multiPart("templateId", template.id().toString())
                        .multiPart("name", "test-doc")
                        .multiPart("jsonOriginString", "{}")
                        .when()
                        .post("/api/documents")
                        .then()
                        .statusCode(201)
                        .extract()
                        .as(DocumentDto.class);

                assertNotNull(doc.id(), "document id must be non-null after generation");
                createdDocumentIds.add(doc.id());
            }

            List<Long> listedIds = given()
                    .when()
                    .get("/api/documents")
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getList("id", Long.class);

            for (Long id : createdDocumentIds) {
                assertTrue(listedIds.contains(id),
                        "GET /api/documents must contain id " + id + " that was just generated");
            }

            assertTrue(listedIds.size() >= createdDocumentIds.size(),
                    "list size must be >= number of documents generated in this run");
        }
    }

    // -------------------------------------------------------------------------
    // Test 6: Non-Existent Resource Returns 404 (documents)
    // -------------------------------------------------------------------------

    @Test
    void nonExistentDocumentReturns404() {
        for (int i = 0; i < 10; i++) {
            long id = 1_000_000L + i + System.nanoTime() % 1000;

            String getBody = given()
                    .when()
                    .get("/api/documents/" + id)
                    .then()
                    .statusCode(404)
                    .extract()
                    .asString();

            assertNotNull(getBody, "error body must not be null for non-existent GET");
            assertFalse(getBody.isBlank(), "error message must be non-empty for non-existent GET");

            String putBody = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("jsonOriginString", "{}")
                    .when()
                    .put("/api/documents/" + id)
                    .then()
                    .statusCode(404)
                    .extract()
                    .asString();

            assertNotNull(putBody, "error body must not be null for non-existent PUT");
            assertFalse(putBody.isBlank(), "error message must be non-empty for non-existent PUT");

            String deleteBody = given()
                    .when()
                    .delete("/api/documents/" + id)
                    .then()
                    .statusCode(404)
                    .extract()
                    .asString();

            assertNotNull(deleteBody, "error body must not be null for non-existent DELETE");
            assertFalse(deleteBody.isBlank(), "error message must be non-empty for non-existent DELETE");
        }
    }

    // -------------------------------------------------------------------------
    // Test 13: Document Update Round-Trip
    // -------------------------------------------------------------------------

    @Test
    // Feature: pdf-man-service, Property 13: Document Update Round-Trip
    // Validates: Requirements 7.1, 7.2, 7.3
    void documentUpdateRoundTrip() {
        for (int i = 0; i < 10; i++) {
            String name = UUID.randomUUID().toString().replace("-", "");
            String content = "Hello World " + i;
            Map<String, Object> newPayload = Map.of("key" + i, "value" + i);

            TemplateDto template = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateType", name)
                    .multiPart("content", content)
                    .when()
                    .post("/api/templates")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(TemplateDto.class);

            assertNotNull(template.id(), "template id must be non-null after creation");

            DocumentDto created = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateId", template.id().toString())
                    .multiPart("name", "test-doc")
                    .multiPart("jsonOriginString", "{}")
                    .when()
                    .post("/api/documents")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(DocumentDto.class);

            assertNotNull(created.id(), "document id must be non-null after generation");
            assertNotNull(created.createdAt(), "createdAt must be non-null after generation");

            DocumentDto updated = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("jsonOriginString", "{\"key" + i + "\": \"value" + i + "\"}")
                    .when()
                    .put("/api/documents/" + created.id())
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(DocumentDto.class);

            assertEquals(created.id(), updated.id(), "id must remain the same after update");
            assertNotNull(updated.updatedAt(), "updatedAt must be non-null after PUT");
            assertFalse(updated.updatedAt().isBefore(created.createdAt()),
                    "updatedAt must be at or after createdAt");
        }
    }

    // -------------------------------------------------------------------------
    // Test 14: Document Delete Round-Trip
    // -------------------------------------------------------------------------

    @Test
    void documentDeleteRoundTrip() {
        for (int i = 0; i < 10; i++) {
            String name = UUID.randomUUID().toString().replace("-", "");
            String content = "Hello World " + i;

            TemplateDto template = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateType", name)
                    .multiPart("content", content)
                    .when()
                    .post("/api/templates")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(TemplateDto.class);

            assertNotNull(template.id(), "template id must be non-null after creation");

            DocumentDto document = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateId", template.id().toString())
                    .multiPart("name", "test-doc")
                    .multiPart("jsonOriginString", "{}")
                    .when()
                    .post("/api/documents")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(DocumentDto.class);

            assertNotNull(document.id(), "document id must be non-null after generation");

            given()
                    .when()
                    .delete("/api/documents/" + document.id())
                    .then()
                    .statusCode(204);

            String errorBody = given()
                    .when()
                    .get("/api/documents/" + document.id())
                    .then()
                    .statusCode(404)
                    .extract()
                    .asString();

            assertNotNull(errorBody, "error body must not be null after delete");
            assertFalse(errorBody.isBlank(), "error message must be non-empty after delete");
        }
    }

    // -------------------------------------------------------------------------
    // Test 15: Storage Backend Failure Returns 503
    // -------------------------------------------------------------------------

    @Test
    void storageBackendFailureReturns503() {
        for (int i = 0; i < 10; i++) {
            String name = UUID.randomUUID().toString().replace("-", "");
            String content = "Hello World " + i;

            TemplateDto template = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateType", name)
                    .multiPart("content", content)
                    .when()
                    .post("/api/templates")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(TemplateDto.class);

            assertNotNull(template.id(), "template id must be non-null after creation");

            DocumentDto document = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateId", template.id().toString())
                    .multiPart("name", "test-doc")
                    .multiPart("jsonOriginString", "{}")
                    .when()
                    .post("/api/documents")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(DocumentDto.class);

            assertNotNull(document.id(), "document id must be non-null after generation");

            mockStorageBackend.setShouldFail(true);
            try {
                String storeError = given()
                        .contentType(ContentType.MULTIPART)
                        .multiPart("templateId", template.id().toString())
                        .multiPart("name", "test-doc")
                        .multiPart("jsonOriginString", "{}")
                        .when()
                        .post("/api/documents")
                        .then()
                        .statusCode(503)
                        .extract()
                        .asString();

                assertNotNull(storeError, "error body must not be null on storage store failure");
                assertFalse(storeError.isBlank(), "error message must be non-empty on storage store failure");

                String retrieveError = given()
                        .when()
                        .get("/api/documents/" + document.id())
                        .then()
                        .statusCode(503)
                        .extract()
                        .asString();

                assertNotNull(retrieveError, "error body must not be null on storage retrieve failure");
                assertFalse(retrieveError.isBlank(), "error message must be non-empty on storage retrieve failure");

                String deleteError = given()
                        .when()
                        .delete("/api/documents/" + document.id())
                        .then()
                        .statusCode(503)
                        .extract()
                        .asString();

                assertNotNull(deleteError, "error body must not be null on storage delete failure");
                assertFalse(deleteError.isBlank(), "error message must be non-empty on storage delete failure");
            } finally {
                mockStorageBackend.setShouldFail(false);
            }
        }
    }
}
