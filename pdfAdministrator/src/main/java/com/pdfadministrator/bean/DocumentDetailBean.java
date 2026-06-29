package com.pdfadministrator.bean;

import com.pdfadministrator.client.PdfManClient;
import com.pdfadministrator.dto.DocumentDto;
import com.pdfadministrator.dto.GeneratePdfRequest;
import com.pdfadministrator.dto.TemplateDto;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CDI backing bean for the document detail / create / edit view.
 */
@Named
@ViewScoped
public class DocumentDetailBean implements Serializable {

    @Inject
    @RestClient
    PdfManClient pdfManClient;

    private Long documentId;
    private boolean editMode;
    private String mode; // "view", "edit", or null (new)

    // Mutable fields for form binding
    private Long id;
    private String name;
    private String description;
    private Long templateId;
    private String createdAt;
    private String createdBy;
    private String updatedAt;
    private String updatedBy;

    // Template dropdown options
    private List<TemplateDto> templates = new java.util.ArrayList<>();
    private String jsonOriginString;
    private String htmlTemplatePreview;
    private String documentHtmlCapture;
    private List<FieldEntry> documentFields = new java.util.ArrayList<>();

    /**
     * Simple POJO for form-bindable field entries.
     */
    public static class FieldEntry implements Serializable {
        private String key;
        private String value;

        public FieldEntry() {}
        public FieldEntry(String key, String value) { this.key = key; this.value = value; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    /**
     * Loads the template list for the dropdown. Called on page load.
     */
    public void loadTemplates() {
        try {
            templates = pdfManClient.listTemplates();
        } catch (WebApplicationException | ProcessingException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Failed to load templates",
                            e.getMessage()));
        }
    }

    /**
     * Called when the user selects a template from the dropdown.
     * Populates the jsonOriginString textarea with the selected template's jsonTemplateString.
     */
    public void onTemplateSelected() {
        if (templateId != null) {
            this.id = templateId;
            templates.stream()
                    .filter(t -> t.getId().equals(templateId))
                    .findFirst()
                    .ifPresent(t -> {
                        String templateContent = t.getJsonTemplateString();
                        System.out.println("=== onTemplateSelected ===");
                        System.out.println("templateId: " + templateId);
                        System.out.println("templateContent starts with '<': " + (templateContent != null && templateContent.trim().startsWith("<")));
                        System.out.println("templateContent (first 100 chars): " + (templateContent != null ? templateContent.substring(0, Math.min(100, templateContent.length())) : "null"));
                        if (templateContent != null && templateContent.trim().startsWith("<")) {
                            // HTML template — show as rendered preview, put empty JSON in textarea
                            htmlTemplatePreview = templateContent;
                            jsonOriginString = "{}";
                            documentFields = new java.util.ArrayList<>();
                        } else {
                            // JSON template — parse fields, show htmlTemplate preview with description
                            jsonOriginString = templateContent;
                            String desc = t.getDescription() != null ? t.getDescription() : t.getTemplateType();
                            String preview = t.getHtmlTemplate();
                            if (preview == null || preview.isBlank()) {
                                preview = "<p style='color:#888'>Save the template to generate HTML preview.</p>";
                            }
                            htmlTemplatePreview = "<h3 style='color:#388557; margin:0 0 10px 0;'>" + desc + "</h3>" + preview;
                            documentFields = parseJsonToFields(templateContent);
                        }
                    });
        } else {
            this.id = null;
            jsonOriginString = null;
            htmlTemplatePreview = null;
        }
    }

    /**
     * Loads an existing document by ID. Called via f:viewAction when an id param is present.
     */
    public void loadDocument(Long id) {
        if (id == null) {
            return;
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        try {
            DocumentDto dto = pdfManClient.listDocuments().stream()
                    .filter(d -> d.getId().equals(id))
                    .findFirst()
                    .orElse(null);
            if (dto != null) {
                populateFromDto(dto);
                this.editMode = true;
            }
        } catch (WebApplicationException e) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Failed to load document",
                            e.getMessage()));
        } catch (ProcessingException e) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Service unavailable",
                            "Could not connect to pdfMan service."));
        }
    }

    /**
     * Saves (creates or updates) the document.
     */
    public void save() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        try {
            // Extract input values from captured HTML and serialize to jsonOriginString
            if (documentHtmlCapture != null && !documentHtmlCapture.isBlank()) {
                System.out.println("=== documentHtmlCapture received (length: " + documentHtmlCapture.length() + ") ===");
                jsonOriginString = extractJsonFromHtml(documentHtmlCapture);
                System.out.println("Extracted jsonOriginString: " + jsonOriginString);
            }

            if (editMode) {
                DocumentDto dto = pdfManClient.updateDocument(this.id, jsonOriginString, documentHtmlCapture);
                populateFromDto(dto);
                facesContext.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Document updated", null));
            } else {
                System.out.println("=== generateDocument parameters ===");
                System.out.println("templateId: " + templateId);
                System.out.println("name: " + name);
                System.out.println("description: " + description);
                System.out.println("jsonOriginString: " + jsonOriginString);
                System.out.println("===================================");
                DocumentDto dto = pdfManClient.generateDocument(templateId, name, description, jsonOriginString, documentHtmlCapture);
                populateFromDto(dto);
                this.editMode = true;
                facesContext.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Document generated", null));
            }
        } catch (WebApplicationException e) {
            e.printStackTrace();
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Save failed",
                            e.getMessage()));
        } catch (ProcessingException e) {
            e.printStackTrace();
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Service unavailable",
                            "Could not connect to pdfMan service."));
        }
    }

    /**
     * Streams the PDF identified by {@code id} to the browser as a file download.
     */
    public void downloadPdf(Long id) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        try {
            Response response = pdfManClient.getPdf(id);
            byte[] pdfBytes = response.readEntity(byte[].class);

            ExternalContext externalContext = facesContext.getExternalContext();
            externalContext.setResponseContentType("application/pdf");
            externalContext.setResponseHeader("Content-Disposition",
                    "attachment; filename=\"document-" + id + ".pdf\"");

            OutputStream out = externalContext.getResponseOutputStream();
            out.write(pdfBytes);
            out.flush();

            facesContext.responseComplete();
        } catch (WebApplicationException | ProcessingException e) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Download failed",
                            e.getMessage()));
        } catch (IOException e) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Download failed",
                            e.getMessage()));
        }
    }

    private void populateFromDto(DocumentDto dto) {
        this.id = dto.getId();
        this.name = dto.getName();
        this.description = dto.getDescription();
        this.jsonOriginString = dto.getJsonOriginString();
        this.templateId = dto.getTemplateId();
        this.createdAt = dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : null;
        this.createdBy = dto.getCreatedBy();
        this.updatedAt = dto.getUpdatedAt() != null ? dto.getUpdatedAt().toString() : null;
        this.updatedBy = dto.getUpdatedBy();
        // Parse fields for editing
        if (jsonOriginString != null && !jsonOriginString.isBlank()) {
            documentFields = parseJsonToFields(jsonOriginString);
        }
        // Load htmlTemplate preview — use documentHtml if it has filled values, else template's htmlTemplate
        String dtoDocHtml = dto.getDocumentHtml();
        if (dtoDocHtml != null && !dtoDocHtml.isBlank()) {
            htmlTemplatePreview = dtoDocHtml;
        } else if (templateId != null && !templates.isEmpty()) {
            templates.stream()
                    .filter(t -> t.getId().equals(templateId))
                    .findFirst()
                    .ifPresent(t -> {
                        String desc = t.getDescription() != null ? t.getDescription() : t.getTemplateType();
                        String preview = t.getHtmlTemplate();
                        if (preview == null || preview.isBlank()) {
                            preview = "<p style='color:#888'>Save the template to generate HTML preview.</p>";
                        }
                        htmlTemplatePreview = "<h3 style='color:#388557; margin:0 0 10px 0;'>" + desc + "</h3>" + preview;
                    });
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /**
     * Parses a JSON string into a list of FieldEntry objects for form binding.
     */
    private List<FieldEntry> parseJsonToFields(String json) {
        List<FieldEntry> fields = new java.util.ArrayList<>();
        if (json != null && !json.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> parsed = mapper.readValue(json, java.util.LinkedHashMap.class);
                for (java.util.Map.Entry<String, Object> entry : parsed.entrySet()) {
                    String value = entry.getValue() != null ? entry.getValue().toString() : "";
                    fields.add(new FieldEntry(entry.getKey(), value));
                }
            } catch (Exception e) {
                // Not valid JSON — return empty list
            }
        }
        return fields;
    }

    /**
     * Serializes the documentFields list back into a JSON string.
     */
    private String serializeFieldsToJson() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        for (FieldEntry entry : documentFields) {
            map.put(entry.getKey(), entry.getValue());
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Extracts field values from the captured HTML by parsing label/input pairs.
     * Returns a JSON string of key-value pairs.
     */
    private String extractJsonFromHtml(String html) {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        // Simple regex to find <label>key</label> followed by <input ... value="val" .../>
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "<label>([^<]+)</label>\\s*<input[^>]*value=\"([^\"]*)\"[^>]*/?>",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            // Unescape HTML entities
            value = value.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"");
            map.put(key, value);
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public boolean isViewMode() {
        return "view".equals(mode);
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getPageTitle() {
        if ("view".equals(mode)) return "View Document";
        if (editMode) return "Edit Document";
        return "New Document";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public List<TemplateDto> getTemplates() {
        return templates;
    }

    public String getJsonOriginString() {
        return jsonOriginString;
    }

    public void setJsonOriginString(String jsonOriginString) {
        this.jsonOriginString = jsonOriginString;
    }

    public String getHtmlTemplatePreview() {
        return htmlTemplatePreview;
    }

    public void setHtmlTemplatePreview(String htmlTemplatePreview) {
        this.htmlTemplatePreview = htmlTemplatePreview;
    }

    public String getDocumentHtmlCapture() {
        return documentHtmlCapture;
    }

    public void setDocumentHtmlCapture(String documentHtmlCapture) {
        this.documentHtmlCapture = documentHtmlCapture;
    }

    public List<FieldEntry> getDocumentFields() {
        return documentFields;
    }

    public void setDocumentFields(List<FieldEntry> documentFields) {
        this.documentFields = documentFields;
    }
}
