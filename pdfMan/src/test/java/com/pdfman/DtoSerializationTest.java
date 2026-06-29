package com.pdfman;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pdfman.dto.DocumentDto;
import com.pdfman.dto.TemplateDto;
import net.jqwik.api.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Property-based tests for DTO serialization round-trip.
 * Feature: pdf-man-service, Property 16: DTO Serialization Round-Trip
 */
class DtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // --- Arbitraries ---

    @Provide
    Arbitrary<Long> longs() {
        return Arbitraries.longs().between(1L, Long.MAX_VALUE);
    }

    @Provide
    Arbitrary<String> nonEmptyStrings() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(100);
    }

    @Provide
    Arbitrary<OffsetDateTime> offsetDateTimes() {
        return Arbitraries.longs()
                .between(0L, 4_000_000_000L)
                .map(epochSecond -> OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC));
    }

    @Provide
    Arbitrary<OffsetDateTime> nullableOffsetDateTimes() {
        return offsetDateTimes().injectNull(0.3);
    }

    @Provide
    Arbitrary<String> nullableStrings() {
        return nonEmptyStrings().injectNull(0.3);
    }

    // --- Property 16: TemplateDto round-trip ---

    @Property(tries = 10)
    // Feature: pdf-man-service, Property 16: DTO Serialization Round-Trip
    // Validates: Requirements 12.1, 12.2, 12.4
    void templateDtoSerializationRoundTrip(
            @ForAll("longs") Long id,
            @ForAll("nonEmptyStrings") String name,
            @ForAll("nonEmptyStrings") String content,
            @ForAll("offsetDateTimes") OffsetDateTime createdAt,
            @ForAll("nullableOffsetDateTimes") OffsetDateTime updatedAt,
            @ForAll("nonEmptyStrings") String createdBy,
            @ForAll("nullableStrings") String updatedBy
    ) throws Exception {
        TemplateDto original = new TemplateDto(id, name, null, content, null, createdAt, updatedAt, createdBy, updatedBy);

        String json = objectMapper.writeValueAsString(original);
        TemplateDto deserialized = objectMapper.readValue(json, TemplateDto.class);

        assertEquals(original.id(), deserialized.id());
        assertEquals(original.templateType(), deserialized.templateType());
        assertEquals(original.jsonTemplateString(), deserialized.jsonTemplateString());
        assertEquals(original.createdAt().toInstant(), deserialized.createdAt().toInstant());
        assertEquals(original.createdBy(), deserialized.createdBy());
        assertEquals(original.updatedBy(), deserialized.updatedBy());
        if (original.updatedAt() == null) {
            assertEquals(null, deserialized.updatedAt());
        } else {
            assertEquals(original.updatedAt().toInstant(), deserialized.updatedAt().toInstant());
        }
    }

    // --- Property 16: DocumentDto round-trip ---

    @Property(tries = 10)
    // Feature: pdf-man-service, Property 16: DTO Serialization Round-Trip
    // Validates: Requirements 12.1, 12.2, 12.4
    void documentDtoSerializationRoundTrip(
            @ForAll("longs") Long id,
            @ForAll("longs") Long templateId,
            @ForAll("nonEmptyStrings") String storageKey,
            @ForAll("offsetDateTimes") OffsetDateTime createdAt,
            @ForAll("nullableOffsetDateTimes") OffsetDateTime updatedAt,
            @ForAll("nonEmptyStrings") String createdBy,
            @ForAll("nullableStrings") String updatedBy
    ) throws Exception {
        DocumentDto original = new DocumentDto(id, templateId, "test-doc", null, "{}", null, storageKey, createdAt, updatedAt, createdBy, updatedBy);

        String json = objectMapper.writeValueAsString(original);
        DocumentDto deserialized = objectMapper.readValue(json, DocumentDto.class);

        assertEquals(original.id(), deserialized.id());
        assertEquals(original.templateId(), deserialized.templateId());
        assertEquals(original.storageKey(), deserialized.storageKey());
        assertEquals(original.createdAt().toInstant(), deserialized.createdAt().toInstant());
        assertEquals(original.createdBy(), deserialized.createdBy());
        assertEquals(original.updatedBy(), deserialized.updatedBy());
        if (original.updatedAt() == null) {
            assertEquals(null, deserialized.updatedAt());
        } else {
            assertEquals(original.updatedAt().toInstant(), deserialized.updatedAt().toInstant());
        }
    }
}

