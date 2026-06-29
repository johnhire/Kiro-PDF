package com.pdfadministrator.bean;

import com.pdfadministrator.client.PdfManClient;
import com.pdfadministrator.dto.TemplateDto;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.Serializable;

/**
 * CDI backing bean for the template create/edit view.
 *
 * <p>When {@code templateId} is {@code null} the bean is in "new template" mode;
 * when it is non-{@code null} the bean is in "edit existing template" mode.
 */
@Named
@ViewScoped
public class TemplateEditBean implements Serializable {

    @Inject
    @RestClient
    PdfManClient pdfManClient;

    private Long templateId;
    private String templateType;
    private String description;
    private String content;
    private String htmlTemplate;
    private String createdAt;
    private String createdBy;
    private String updatedAt;
    private String updatedBy;
    private TemplateDto template;

    /**
     * Called by preRenderView event. Loads the template if templateId was set by the view param.
     */
    public void init() {
        if (templateId != null && template == null) {
            loadTemplate(templateId);
        }
    }

    /**
     * Loads an existing template by ID and populates the form fields.
     *
     * @param id the ID of the template to load
     */
    public void loadTemplate(Long id) {
        this.templateId = id;
        try {
            template = pdfManClient.getTemplate(id);
            templateType = template.getTemplateType();
            description = template.getDescription();
            content = template.getJsonTemplateString();
            htmlTemplate = template.getHtmlTemplate();
            createdAt = template.getCreatedAt() != null ? template.getCreatedAt().toString() : null;
            createdBy = template.getCreatedBy();
            updatedAt = template.getUpdatedAt() != null ? template.getUpdatedAt().toString() : null;
            updatedBy = template.getUpdatedBy();
        } catch (WebApplicationException | ProcessingException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error loading template",
                            e.getMessage()));
        }
    }

    /**
     * Saves the template — creates a new one when {@code templateId} is
     * {@code null}, or updates the existing one otherwise.
     */
    public void save() {
        if (templateId == null) {
            // Create new template
            try {
                TemplateDto created = pdfManClient.createTemplate(templateType, description, content);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Template created",
                                created.getTemplateType()));
                templateType = null;
                content = null;
            } catch (WebApplicationException e) {
                String detail = e.getMessage();
                try {
                    if (e.getResponse() != null) {
                        String body = e.getResponse().readEntity(String.class);
                        if (body != null && !body.isBlank()) {
                            detail = body;
                        }
                    }
                } catch (Exception ignored) {
                    // Fall back to the exception message already captured above
                }
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Save failed",
                                detail));
            } catch (ProcessingException e) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Service unavailable",
                                e.getMessage()));
            }
        } else {
            // Update existing template
            try {
                pdfManClient.updateTemplate(templateId, description, content);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Template updated",
                                null));
            } catch (WebApplicationException e) {
                String detail = e.getMessage();
                try {
                    if (e.getResponse() != null) {
                        String body = e.getResponse().readEntity(String.class);
                        if (body != null && !body.isBlank()) {
                            detail = body;
                        }
                    }
                } catch (Exception ignored) {
                    // Fall back to the exception message already captured above
                }
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Save failed",
                                detail));
            } catch (ProcessingException e) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Service unavailable",
                                e.getMessage()));
            }
        }
    }

    /**
     * Returns {@code true} when editing an existing template (i.e. {@code templateId} is set).
     */
    public boolean isEditMode() {
        return templateId != null;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getHtmlTemplate() {
        return htmlTemplate;
    }

    public void setHtmlTemplate(String htmlTemplate) {
        this.htmlTemplate = htmlTemplate;
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

    public TemplateDto getTemplate() {
        return template;
    }

    public void setTemplate(TemplateDto template) {
        this.template = template;
    }
}
