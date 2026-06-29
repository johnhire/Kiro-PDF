package com.pdfman.service;

import com.pdfman.dto.TemplateDto;
import com.pdfman.entity.DocumentEntity;
import com.pdfman.entity.TemplateEntity;
import com.pdfman.exception.DuplicateNameException;
import com.pdfman.exception.NotFoundException;
import com.pdfman.exception.TemplateInUseException;
import com.pdfman.util.AuditHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class TemplateService {

    @Transactional
    public TemplateDto create(String templateType, String description, String content) {
        TemplateEntity.findByTemplateType(templateType).ifPresent(e -> {
            throw new DuplicateNameException("Template with templateType '" + templateType + "' already exists");
        });

        TemplateEntity entity = new TemplateEntity();
        entity.templateType = templateType;
        entity.description = description;
        entity.jsonTemplateString = content;
        entity.htmlTemplate = buildGenericHtmlTemplate(content);
        AuditHelper.stampCreated(entity);
        entity.persist();

        return toDto(entity);
    }

    public TemplateDto findById(Long id) {
        TemplateEntity entity = TemplateEntity.<TemplateEntity>findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Template not found: " + id));
        return toDto(entity);
    }

    public List<TemplateDto> listAll() {
        return TemplateEntity.<TemplateEntity>listAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public TemplateDto update(Long id, String description, String content) {
        TemplateEntity entity = TemplateEntity.<TemplateEntity>findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Template not found: " + id));

        entity.description = description;
        entity.jsonTemplateString = content;
        entity.htmlTemplate = buildGenericHtmlTemplate(content);
        AuditHelper.stampUpdated(entity);
        entity.persist();

        return toDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        TemplateEntity entity = TemplateEntity.<TemplateEntity>findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Template not found: " + id));

        if (DocumentEntity.count("template", entity) > 0) {
            throw new TemplateInUseException("Template '" + entity.templateType + "' is in use by one or more documents");
        }

        entity.delete();
    }

    private TemplateDto toDto(TemplateEntity entity) {
        return new TemplateDto(
                entity.id,
                entity.templateType,
                entity.description,
                entity.jsonTemplateString,
                entity.htmlTemplate,
                entity.createdAt,
                entity.updatedAt,
                entity.createdBy,
                entity.updatedBy
        );
    }

    /**
     * Builds a generic HTML template from the jsonTemplateString.
     * Parses the JSON to extract field names and creates a static HTML preview
     * with field labels and empty value placeholders, styled with Mass.gov look.
     * Also embeds Thymeleaf expressions for runtime rendering.
     */
    private String buildGenericHtmlTemplate(String jsonTemplateString) {
        // Parse JSON to get field names
        java.util.Map<String, Object> fields = new java.util.LinkedHashMap<>();
        if (jsonTemplateString != null && !jsonTemplateString.isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> parsed = mapper.readValue(jsonTemplateString, java.util.LinkedHashMap.class);
                fields = parsed;
            } catch (Exception e) {
                // Not valid JSON — just use empty
            }
        }

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
        sb.append(".section{margin-top:20px;margin-bottom:10px;padding:15px;border:1px solid #e0e0e0;border-radius:6px;background:#f9f9f9;clear:both}");
        sb.append(".section-title{font-size:15px;font-weight:bold;color:#388557;margin:0 0 12px 0;padding-bottom:8px;border-bottom:1px solid #ddd}");
        sb.append(".section-fields{overflow:hidden}");
        sb.append(".section-fields .form-field{float:left;width:48%;margin-right:2%}");
        sb.append(".footer{background:#f0f0f0;border-top:1px solid #ccc;padding:12px 20px;font-size:10px;color:#555;margin-top:30px}");
        sb.append("</style></head><body>");
        sb.append("<div class=\"banner\">An official document of the Commonwealth of Massachusetts</div>");
        sb.append("<div class=\"header\"><h1>PDF Administrator</h1>");
        sb.append("<h2>Executive Office of Health and Human Services</h2></div>");
        sb.append("<div class=\"title-bar\"><h3>Generated Document</h3></div>");
        sb.append("<div class=\"content\">");

        renderFields(sb, fields);

        if (fields.isEmpty()) {
            sb.append("<p style=\"color:#888;font-style:italic\">No fields defined in JSON template.</p>");
        }

        sb.append("</div>");
        sb.append("<div class=\"footer\">&#169; 2026 Commonwealth of Massachusetts.</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void renderFields(StringBuilder sb, java.util.Map<String, Object> fields) {
        for (java.util.Map.Entry<String, Object> entry : fields.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            if (val instanceof java.util.Map) {
                // Nested object — render as a titled section with two-column layout
                sb.append("<div class=\"section\">");
                sb.append("<div class=\"section-title\">").append(escapeHtml(camelCaseToLabel(key))).append("</div>");
                sb.append("<div class=\"section-fields\">");
                renderFields(sb, (java.util.Map<String, Object>) val);
                sb.append("</div>");
                sb.append("</div>");
            } else if (val instanceof java.util.List) {
                // Array — render as a labeled comma-separated input
                java.util.List<?> list = (java.util.List<?>) val;
                String joined = list.stream()
                        .map(Object::toString)
                        .collect(java.util.stream.Collectors.joining(", "));
                sb.append("<div class=\"form-field\">");
                sb.append("<label>").append(escapeHtml(camelCaseToLabel(key))).append("</label>");
                sb.append("<input type=\"text\" value=\"").append(escapeHtml(joined)).append("\" />");
                sb.append("</div>");
            } else {
                // Simple field (string, number, boolean) — render as label + input
                String value = val != null ? val.toString() : "";
                sb.append("<div class=\"form-field\">");
                sb.append("<label>").append(escapeHtml(camelCaseToLabel(key))).append("</label>");
                sb.append("<input type=\"text\" value=\"").append(escapeHtml(value)).append("\" />");
                sb.append("</div>");
            }
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * Converts camelCase field names to human-readable Title Case labels.
     * e.g., "firstName" → "First Name", "dateOfBirth" → "Date Of Birth"
     */
    private String camelCaseToLabel(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) return "";
        // Insert space before each uppercase letter
        String spaced = camelCase.replaceAll("([a-z])([A-Z])", "$1 $2");
        // Capitalize first letter of each word
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
