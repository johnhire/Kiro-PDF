package com.pdfman;

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
 * Tests for TemplateResource.
 * Feature: pdf-man-service
 */
@QuarkusTest
class TemplateResourceTest {

    @Inject
    MockStorageBackend mockStorageBackend;

    @BeforeEach
    void resetStorage() {
        mockStorageBackend.reset();
    }

    // -------------------------------------------------------------------------
    // Test 1: Template Creation Round-Trip
    // -------------------------------------------------------------------------

    @Test
    void templateCreationRoundTrip() {
        for (int i = 0; i < 10; i++) {
            String name = UUID.randomUUID().toString().replace("-", "");
            String content = "Hello World " + i;

            TemplateDto created = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateType", name)
                    .multiPart("content", content)
                    .when()
                    .post("/api/templates")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(TemplateDto.class);

            assertNotNull(created.id(), "id must be non-null after creation");

            TemplateDto fetched = given()
                    .when()
                    .get("/api/templates/" + created.id())
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(TemplateDto.class);

            assertEquals(name, fetched.templateType(), "name must match the posted value");
            assertEquals(content, fetched.jsonTemplateString(), "content must match the posted value");
        }
    }

    // -------------------------------------------------------------------------
    // Test 2: Template Creation with Missing Fields Returns 400
    // -------------------------------------------------------------------------

    @Test
    void missingFieldsReturns400() {
        for (int i = 0; i < 10; i++) {
            String name = UUID.randomUUID().toString().replace("-", "");
            String content = "Hello World " + i;

            String bodyMissingName = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("content", content)
                    .when()
                    .post("/api/templates")
                    .then()
                    .statusCode(400)
                    .extract()
                    .asString();

            assertNotNull(bodyMissingName, "error body must not be null when name is missing");
            assertFalse(bodyMissingName.isBlank(), "error message must be non-empty when name is missing");

            String bodyMissingContent = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateType", name)
                    .when()
                    .post("/api/templates")
                    .then()
                    .statusCode(400)
                    .extract()
                    .asString();

            assertNotNull(bodyMissingContent, "error body must not be null when content is missing");
            assertFalse(bodyMissingContent.isBlank(), "error message must be non-empty when content is missing");
        }
    }

    // -------------------------------------------------------------------------
    // Test 3: Duplicate Template Name Returns 409
    // -------------------------------------------------------------------------

    @Test
    void duplicateTemplateNameReturns409() {
        for (int i = 0; i < 10; i++) {
            String name = UUID.randomUUID().toString().replace("-", "");
            String firstContent = "Hello World " + i;
            String secondContent = "Updated Content " + i;

            given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateType", name)
                    .multiPart("content", firstContent)
                    .when()
                    .post("/api/templates")
                    .then()
                    .statusCode(201);

            String errorBody = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateType", name)
                    .multiPart("content", secondContent)
                    .when()
                    .post("/api/templates")
                    .then()
                    .statusCode(409)
                    .extract()
                    .asString();

            assertNotNull(errorBody, "error body must not be null on duplicate name");
            assertFalse(errorBody.isBlank(), "error message must be non-empty on duplicate name");
        }
    }

    // -------------------------------------------------------------------------
    // Test 5: Template List Contains All Created Templates
    // -------------------------------------------------------------------------

    @Test
    void templateListContainsAllCreatedTemplates() {
        for (int i = 0; i < 10; i++) {
            List<String[]> templates = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                templates.add(new String[]{
                        UUID.randomUUID().toString().replace("-", ""),
                        "Hello World " + i + "_" + j
                });
            }

            List<Long> createdIds = new ArrayList<>();
            for (String[] nameAndContent : templates) {
                TemplateDto created = given()
                        .contentType(ContentType.MULTIPART)
                        .multiPart("templateType", nameAndContent[0])
                        .multiPart("content", nameAndContent[1])
                        .when()
                        .post("/api/templates")
                        .then()
                        .statusCode(201)
                        .extract()
                        .as(TemplateDto.class);

                assertNotNull(created.id(), "id must be non-null after creation");
                createdIds.add(created.id());
            }

            List<Long> listedIds = given()
                    .when()
                    .get("/api/templates")
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getList("id", Long.class);

            for (Long id : createdIds) {
                assertTrue(listedIds.contains(id),
                        "GET /api/templates must contain id " + id + " that was just created");
            }

            assertTrue(listedIds.size() >= createdIds.size(),
                    "list size must be >= number of templates created in this run");
        }
    }

    // -------------------------------------------------------------------------
    // Test 6: Non-Existent Resource Returns 404
    // -------------------------------------------------------------------------

    @Test
    void nonExistentTemplateReturns404() {
        for (int i = 0; i < 10; i++) {
            long id = 1_000_000L + i + System.nanoTime() % 1000;

            String getBody = given()
                    .when()
                    .get("/api/templates/" + id)
                    .then()
                    .statusCode(404)
                    .extract()
                    .asString();

            assertNotNull(getBody, "error body must not be null for non-existent GET");
            assertFalse(getBody.isBlank(), "error message must be non-empty for non-existent GET");

            String putBody = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("content", "updated content")
                    .when()
                    .put("/api/templates/" + id)
                    .then()
                    .statusCode(404)
                    .extract()
                    .asString();

            assertNotNull(putBody, "error body must not be null for non-existent PUT");
            assertFalse(putBody.isBlank(), "error message must be non-empty for non-existent PUT");

            String deleteBody = given()
                    .when()
                    .delete("/api/templates/" + id)
                    .then()
                    .statusCode(404)
                    .extract()
                    .asString();

            assertNotNull(deleteBody, "error body must not be null for non-existent DELETE");
            assertFalse(deleteBody.isBlank(), "error message must be non-empty for non-existent DELETE");
        }
    }

    // -------------------------------------------------------------------------
    // Test 7: Template Update Round-Trip
    // -------------------------------------------------------------------------

    @Test
    void templateUpdateRoundTrip() {
        for (int i = 0; i < 10; i++) {
            String name = UUID.randomUUID().toString().replace("-", "");
            String originalContent = "Hello World " + i;
            String updatedContent = "Updated Content " + i;

            TemplateDto created = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateType", name)
                    .multiPart("content", originalContent)
                    .when()
                    .post("/api/templates")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(TemplateDto.class);

            assertNotNull(created.id(), "id must be non-null after creation");

            TemplateDto updated = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("content", updatedContent)
                    .when()
                    .put("/api/templates/" + created.id())
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(TemplateDto.class);

            assertEquals(created.id(), updated.id(), "id must remain the same after update");
            assertEquals(name, updated.templateType(), "name must remain unchanged after update");
            assertEquals(updatedContent, updated.jsonTemplateString(), "PUT response must reflect the new content");
            assertNotNull(updated.updatedAt(), "updatedAt must be set after update");
            assertNotNull(updated.updatedBy(), "updatedBy must be set after update");

            TemplateDto fetched = given()
                    .when()
                    .get("/api/templates/" + created.id())
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(TemplateDto.class);

            assertEquals(updatedContent, fetched.jsonTemplateString(), "GET after PUT must return the updated content");
            assertEquals(name, fetched.templateType(), "GET after PUT must return the original name");
        }
    }

    // -------------------------------------------------------------------------
    // Test 8: Template Delete Round-Trip
    // -------------------------------------------------------------------------

    @Test
    void templateDeleteRoundTrip() {
        for (int i = 0; i < 10; i++) {
            String name = UUID.randomUUID().toString().replace("-", "");
            String content = "Hello World " + i;

            TemplateDto created = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateType", name)
                    .multiPart("content", content)
                    .when()
                    .post("/api/templates")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(TemplateDto.class);

            assertNotNull(created.id(), "id must be non-null after creation");

            given()
                    .when()
                    .delete("/api/templates/" + created.id())
                    .then()
                    .statusCode(204);

            String errorBody = given()
                    .when()
                    .get("/api/templates/" + created.id())
                    .then()
                    .statusCode(404)
                    .extract()
                    .asString();

            assertNotNull(errorBody, "error body must not be null after deletion");
            assertFalse(errorBody.isBlank(), "error message must be non-empty after deletion");
        }
    }

    // -------------------------------------------------------------------------
    // Test 9: Template In-Use Delete Returns 409
    // -------------------------------------------------------------------------

    @Test
    void templateInUseDeleteReturns409() {
        for (int i = 0; i < 10; i++) {
            String name = UUID.randomUUID().toString().replace("-", "");
            String content = "Hello World " + i;

            TemplateDto created = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateType", name)
                    .multiPart("content", content)
                    .when()
                    .post("/api/templates")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(TemplateDto.class);

            assertNotNull(created.id(), "id must be non-null after creation");

            given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateId", created.id().toString())
                    .multiPart("name", "test-doc")
                    .multiPart("jsonOriginString", "{}")
                    .when()
                    .post("/api/documents")
                    .then()
                    .statusCode(201);

            String errorBody = given()
                    .when()
                    .delete("/api/templates/" + created.id())
                    .then()
                    .statusCode(409)
                    .extract()
                    .asString();

            assertNotNull(errorBody, "error body must not be null when deleting an in-use template");
            assertFalse(errorBody.isBlank(), "error message must be non-empty when deleting an in-use template");
        }
    }

    // -------------------------------------------------------------------------
    // Test 4: Audit Fields Present on All Created and Updated Entities
    // -------------------------------------------------------------------------

    @Test
    void auditFieldsPresentOnCreateAndUpdate() {
        for (int i = 0; i < 10; i++) {
            String name = UUID.randomUUID().toString().replace("-", "");
            String originalContent = "Hello World " + i;
            String updatedContent = "Updated Content " + i;

            TemplateDto created = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateType", name)
                    .multiPart("content", originalContent)
                    .when()
                    .post("/api/templates")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(TemplateDto.class);

            assertNotNull(created.createdAt(), "template createdAt must be non-null after creation");
            assertNotNull(created.createdBy(), "template createdBy must be non-null after creation");
            assertNull(created.updatedAt(), "template updatedAt must be null after creation");
            assertNull(created.updatedBy(), "template updatedBy must be null after creation");

            TemplateDto updated = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("content", updatedContent)
                    .when()
                    .put("/api/templates/" + created.id())
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(TemplateDto.class);

            assertNotNull(updated.updatedAt(), "template updatedAt must be non-null after update");
            assertNotNull(updated.updatedBy(), "template updatedBy must be non-null after update");
            assertFalse(updated.updatedAt().isBefore(created.createdAt()),
                    "template updatedAt must be at or after createdAt");

            com.pdfman.dto.DocumentDto docCreated = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("templateId", created.id().toString())
                    .multiPart("name", "test-doc")
                    .multiPart("jsonOriginString", "{}")
                    .when()
                    .post("/api/documents")
                    .then()
                    .statusCode(201)
                    .extract()
                    .as(com.pdfman.dto.DocumentDto.class);

            assertNotNull(docCreated.createdAt(), "document createdAt must be non-null after creation");
            assertNotNull(docCreated.createdBy(), "document createdBy must be non-null after creation");
            assertNull(docCreated.updatedAt(), "document updatedAt must be null after creation");
            assertNull(docCreated.updatedBy(), "document updatedBy must be null after creation");

            com.pdfman.dto.DocumentDto docUpdated = given()
                    .contentType(ContentType.MULTIPART)
                    .multiPart("jsonOriginString", "{}")
                    .when()
                    .put("/api/documents/" + docCreated.id())
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(com.pdfman.dto.DocumentDto.class);

            assertNotNull(docUpdated.updatedAt(), "document updatedAt must be non-null after update");
            assertNotNull(docUpdated.updatedBy(), "document updatedBy must be non-null after update");
            assertFalse(docUpdated.updatedAt().isBefore(docCreated.createdAt()),
                    "document updatedAt must be at or after createdAt");
        }
    }
}
