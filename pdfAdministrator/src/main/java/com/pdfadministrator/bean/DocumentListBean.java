package com.pdfadministrator.bean;

import com.pdfadministrator.client.PdfManClient;
import com.pdfadministrator.dto.DocumentDto;
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
 * CDI backing bean for the document list view.
 *
 * <p>Loads all document metadata from the pdfMan service on view construction.
 * On connectivity failure the list is set to empty and a user-visible error
 * message is added so the page is never left blank.
 */
@Named
@ViewScoped
public class DocumentListBean implements Serializable {

    @Inject
    @RestClient
    PdfManClient pdfManClient;

    private List<DocumentDto> documents;

    @PostConstruct
    public void init() {
        loadDocuments();
    }

    /**
     * Fetches the full document list from the remote service.
     *
     * <ul>
     *   <li>On a {@link WebApplicationException} (HTTP error response) an error
     *       message with summary "Error loading documents" is added.</li>
     *   <li>On a {@link ProcessingException} (connectivity failure) an error
     *       message with summary "Service unavailable — could not connect to pdfMan"
     *       is added so the page is never left blank.</li>
     * </ul>
     */
    public void loadDocuments() {
        try {
            documents = pdfManClient.listDocuments();
        } catch (WebApplicationException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error loading documents",
                            e.getMessage()));
            documents = new ArrayList<>();
        } catch (ProcessingException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Service unavailable — could not connect to pdfMan",
                            e.getMessage()));
            documents = new ArrayList<>();
        }
    }

    public List<DocumentDto> getDocuments() {
        return documents;
    }

    /**
     * Deletes a document by ID and refreshes the list.
     */
    public void deleteDocument(Long id) {
        try {
            pdfManClient.deleteDocument(id);
            loadDocuments();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Document deleted", null));
        } catch (WebApplicationException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Delete failed",
                            e.getMessage()));
        } catch (ProcessingException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Service unavailable",
                            e.getMessage()));
        }
    }
}
