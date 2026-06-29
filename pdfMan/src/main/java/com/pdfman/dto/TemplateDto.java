package com.pdfman.dto;

import java.time.OffsetDateTime;

public record TemplateDto(
        Long id,
        String templateType,
        String description,
        String jsonTemplateString,
        String htmlTemplate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String createdBy,
        String updatedBy
) {}
