package com.pdfman.service;

import com.pdfman.dto.DocumentDto;
import com.pdfman.entity.DocumentEntity;
import com.pdfman.entity.TemplateEntity;
import com.pdfman.exception.NotFoundException;
import com.pdfman.storage.StorageBackend;
import com.pdfman.util.AuditHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class DocumentService {

    @Inject
    StorageBackend storageBackend;

    @Inject
    PdfRenderingService pdfRenderingService;

    @Transactional
    public DocumentDto generate(Long templateId, String name, String description, String jsonOriginString, String documentHtml) {
        TemplateEntity template = TemplateEntity.<TemplateEntity>findByIdOptional(templateId)
                .orElseThrow(() -> new NotFoundException("Template not found: " + templateId));

        // Parse jsonOriginString into a payload map for rendering
        Map<String, Object> payload = new java.util.HashMap<>();
        if (jsonOriginString != null && !jsonOriginString.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = mapper.readValue(jsonOriginString, Map.class);
                payload = parsed;
            } catch (Exception e) {
                e.printStackTrace();
                throw new com.pdfman.exception.InvalidPayloadException("Invalid JSON in jsonOriginString: " + e.getMessage());
            }
        }

        // Use documentHtml if provided (filled HTML from user), else template's htmlTemplate, else jsonTemplateString
        String renderTemplate;
        if (documentHtml != null && !documentHtml.isBlank()) {
            renderTemplate = documentHtml;
        } else if (template.htmlTemplate != null && !template.htmlTemplate.isBlank()) {
            renderTemplate = template.htmlTemplate;
        } else {
            renderTemplate = template.jsonTemplateString;
        }
        byte[] pdfBytes = pdfRenderingService.render(renderTemplate, payload);

        String storageKey = "documents/" + name + ".pdf";
        //String storageKey = UUID.randomUUID() + ".pdf";
        storageBackend.store(storageKey, pdfBytes, "application/pdf");

        DocumentEntity entity = new DocumentEntity();
        entity.template = template;
        entity.name = (name != null && !name.isBlank()) ? name : template.templateType + "-" + UUID.randomUUID().toString().substring(0, 8);
        entity.description = description;
        entity.jsonOriginString = jsonOriginString;
        entity.documentHtml = (documentHtml != null && !documentHtml.isBlank()) ? documentHtml : buildGenericHtmlTemplate(payload);
        entity.storageKey = storageKey;
        AuditHelper.stampCreated(entity);
        entity.persist();

        return toDto(entity);
    }

    public DocumentEntity findById(Long id) {
        return DocumentEntity.<DocumentEntity>findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Document not found: " + id));
    }

    public List<DocumentDto> listAll() {
        return DocumentEntity.<DocumentEntity>listAll().stream()
                .map(this::toDto)
                .toList();
    }

    public byte[] getPdfBytes(Long id) {
        DocumentEntity entity = findById(id);
        return storageBackend.retrieve(entity.storageKey);
    }

    @Transactional
    public DocumentDto update(Long id, String jsonOriginString, String documentHtml) {
        DocumentEntity entity = findById(id);

        // Parse jsonOriginString for rendering
        Map<String, Object> payload = new java.util.HashMap<>();
        if (jsonOriginString != null && !jsonOriginString.isBlank() && !jsonOriginString.trim().equals("{}")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = mapper.readValue(jsonOriginString, Map.class);
                payload = parsed;
            } catch (Exception e) {
                payload.put("content", jsonOriginString);
            }
        }

        // Use documentHtml for rendering if available
        String renderTemplate;
        if (documentHtml != null && !documentHtml.isBlank()) {
            renderTemplate = documentHtml;
        } else if (entity.template.htmlTemplate != null) {
            renderTemplate = entity.template.htmlTemplate;
        } else {
            renderTemplate = entity.template.jsonTemplateString;
        }

        byte[] pdfBytes = pdfRenderingService.render(renderTemplate, payload);
        storageBackend.store(entity.storageKey, pdfBytes, "application/pdf");

        entity.jsonOriginString = jsonOriginString;
        entity.documentHtml = (documentHtml != null && !documentHtml.isBlank()) ? documentHtml : buildGenericHtmlTemplate(payload);
        AuditHelper.stampUpdated(entity);
        entity.persist();

        return toDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        DocumentEntity entity = findById(id);
        storageBackend.delete(entity.storageKey);
        entity.delete();
    }

    private DocumentDto toDto(DocumentEntity entity) {
        return new DocumentDto(
                entity.id,
                entity.template.id,
                entity.name,
                entity.description,
                entity.jsonOriginString,
                entity.documentHtml,
                entity.storageKey,
                entity.createdAt,
                entity.updatedAt,
                entity.createdBy,
                entity.updatedBy
        );
    }

    /**
     * Builds a generic Thymeleaf HTML template from the JSON payload keys/values.
     * Uses the Mass.gov styling with header, table of fields, and footer.
     */
    private String buildGenericHtmlTemplate(Map<String, Object> payload) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><style>");
        sb.append("body{font-family:Arial,sans-serif;margin:0;padding:0;color:#333}");
        sb.append(".banner{background:#1a1a2e;color:#fff;padding:8px 20px;font-size:11px}");
        sb.append(".header{background:#14558f;color:#fff;padding:15px 20px}");
        sb.append(".header h1{margin:0;font-size:18px;font-weight:normal}");
        sb.append(".header h2{margin:4px 0 0 0;font-size:11px;color:#388557;font-weight:bold;text-transform:uppercase}");
        sb.append(".title-bar{padding:15px 20px;border-bottom:2px solid #388557}");
        sb.append(".title-bar h3{margin:0;font-size:16px;color:#388557}");
        sb.append(".content{padding:20px}");
        sb.append(".form-field{margin-bottom:16px}");
        sb.append(".form-field label{display:block;font-weight:bold;color:#14558f;margin-bottom:4px;font-size:13px}");
        sb.append(".form-field input{width:100%;padding:8px 10px;border:1px solid #ccc;border-radius:4px;font-size:14px;box-sizing:border-box}");
        sb.append(".footer{background:#f0f0f0;border-top:1px solid #ccc;padding:12px 20px;font-size:10px;color:#555;margin-top:30px}");
        sb.append("</style></head><body>");
        sb.append("<div class=\"banner\">An official document of the Commonwealth of Massachusetts</div>");
        sb.append("<div class=\"header\"><h1>PDF Administrator</h1>");
        sb.append("<h2>Executive Office of Health and Human Services</h2></div>");
        sb.append("<div class=\"title-bar\"><h3>Generated Document</h3></div>");
        sb.append("<div class=\"content\">");

        if (payload != null) {
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                sb.append("<div class=\"form-field\">");
                sb.append("<label>").append(escapeHtml(camelCaseToLabel(key))).append("</label>");
                sb.append("<input type=\"text\" value=\"").append(escapeHtml(value)).append("\" />");
                sb.append("</div>");
            }
        }

        sb.append("</div>");
        sb.append("<div class=\"footer\">&#169; 2026 Commonwealth of Massachusetts.</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String camelCaseToLabel(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) return "";
        String spaced = camelCase.replaceAll("([a-z])([A-Z])", "$1 $2");
        StringBuilder result = new StringBuilder();
        for (String word : spaced.split(" ")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) result.append(word.substring(1));
                result.append(" ");
            }
        }
        return result.toString().trim();
    }
}
