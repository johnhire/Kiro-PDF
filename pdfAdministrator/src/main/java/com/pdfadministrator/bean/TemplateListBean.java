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
import java.util.ArrayList;
import java.util.List;

/**
 * CDI backing bean for the template list view.
 *
 * <p>Loads all templates from the pdfMan service on view construction and
 * exposes a delete action that refreshes the list after a successful deletion.
 */
@Named
@ViewScoped
public class TemplateListBean implements Serializable {

    @Inject
    @RestClient
    PdfManClient pdfManClient;

    private List<TemplateDto> templates;

    @PostConstruct
    public void init() {
        loadTemplates();
    }

    /**
     * Fetches the full template list from the remote service.
     * On any communication or server error a SEVERITY_ERROR FacesMessage is
     * added and {@code templates} is reset to an empty list.
     */
    public void loadTemplates() {
        try {
            templates = pdfManClient.listTemplates();
        } catch (WebApplicationException | ProcessingException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error loading templates",
                            e.getMessage()));
            templates = new ArrayList<>();
        }
    }

    /**
     * Deletes the template with the given {@code id} and refreshes the list.
     *
     * @param id the ID of the template to delete
     */
    public void deleteTemplate(Long id) {
        try {
            pdfManClient.deleteTemplate(id);
            loadTemplates();
        } catch (WebApplicationException e) {
            String detail = e.getMessage();
            try {
                // Attempt to extract a more descriptive message from the response body
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
                            "Delete failed",
                            detail));
        } catch (ProcessingException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Service unavailable",
                            e.getMessage()));
        }
    }

    public List<TemplateDto> getTemplates() {
        return templates;
    }
}
