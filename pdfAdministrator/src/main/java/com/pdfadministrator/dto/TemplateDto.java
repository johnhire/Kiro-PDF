package com.pdfadministrator.dto;

import java.time.OffsetDateTime;

public class TemplateDto {

    private Long id;
    private String templateType;
    private String description;
    private String jsonTemplateString;
    private String htmlTemplate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public TemplateDto() {}

    public TemplateDto(Long id, String templateType, String jsonTemplateString,
                       OffsetDateTime createdAt, OffsetDateTime updatedAt,
                       String createdBy, String updatedBy) {
        this.id = id;
        this.templateType = templateType;
        this.jsonTemplateString = jsonTemplateString;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getJsonTemplateString() { return jsonTemplateString; }
    public void setJsonTemplateString(String jsonTemplateString) { this.jsonTemplateString = jsonTemplateString; }

    public String getHtmlTemplate() { return htmlTemplate; }
    public void setHtmlTemplate(String htmlTemplate) { this.htmlTemplate = htmlTemplate; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
