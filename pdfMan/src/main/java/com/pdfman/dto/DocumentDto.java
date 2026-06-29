package com.pdfman.dto;

import java.time.OffsetDateTime;

public record DocumentDto(
        Long id,
        Long templateId,
        String name,
        String description,
        String jsonOriginString,
        String documentHtml,
        String storageKey,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {}
