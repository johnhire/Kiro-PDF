package com.pdfadministrator.dto;

import java.time.OffsetDateTime;

public class DocumentDto {

    private Long id;
    private Long templateId;
    private String name;
    private String description;
    private String jsonOriginString;
    private String documentHtml;
    private String storageKey;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public DocumentDto() {}

    public DocumentDto(Long id, Long templateId, String name, String description,
                       String storageKey, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                       String createdBy, String updatedBy) {
        this.id = id;
        this.templateId = templateId;
        this.name = name;
        this.description = description;
        this.storageKey = storageKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getJsonOriginString() { return jsonOriginString; }
    public void setJsonOriginString(String jsonOriginString) { this.jsonOriginString = jsonOriginString; }

    public String getDocumentHtml() { return documentHtml; }
    public void setDocumentHtml(String documentHtml) { this.documentHtml = documentHtml; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
