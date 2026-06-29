package com.pdfman.util;

import com.pdfman.entity.DocumentEntity;
import com.pdfman.entity.TemplateEntity;

import java.time.OffsetDateTime;

public class AuditHelper {

    private AuditHelper() {}

    public static void stampCreated(TemplateEntity entity) {
        OffsetDateTime now = OffsetDateTime.now();
        entity.createdAt = now;
        entity.updatedAt = null;
        entity.createdBy = "system";
        entity.updatedBy = null;
    }

    public static void stampCreated(DocumentEntity entity) {
        OffsetDateTime now = OffsetDateTime.now();
        entity.createdAt = now;
        entity.updatedAt = null;
        entity.createdBy = "system";
        entity.updatedBy = null;
    }

    public static void stampUpdated(TemplateEntity entity) {
        entity.updatedAt = OffsetDateTime.now();
        entity.updatedBy = "system";
    }

    public static void stampUpdated(DocumentEntity entity) {
        entity.updatedAt = OffsetDateTime.now();
        entity.updatedBy = "system";
    }
}
