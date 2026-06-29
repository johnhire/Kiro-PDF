package com.pdfadministrator.client;

import com.pdfadministrator.dto.DocumentDto;
import com.pdfadministrator.dto.GeneratePdfRequest;
import com.pdfadministrator.dto.TemplateDto;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;

import java.util.List;

/**
 * MicroProfile REST Client mirroring all pdfMan REST endpoints.
 *
 * <p>Base URL is configured via the {@code pdf-man-client} config key:
 * <pre>
 *   quarkus.rest-client.pdf-man-client.url=http://localhost:8080
 * </pre>
 * or via the legacy MP REST Client property:
 * <pre>
 *   com.pdfadministrator.client.PdfManClient/mp-rest/url=http://localhost:8080
 * </pre>
 */
@RegisterRestClient(configKey = "pdf-man-client")
public interface PdfManClient {

    // -------------------------------------------------------------------------
    // Template endpoints — /api/templates
    // -------------------------------------------------------------------------

    /**
     * POST /api/templates — upload a new template (multipart: templateType + content).
     * Returns 201 with the created {@link TemplateDto}.
     */
    @POST
    @Path("/api/templates")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    TemplateDto createTemplate(@RestForm String templateType, @RestForm String description, @RestForm String content);

    /**
     * GET /api/templates — list all templates.
     * Returns 200 with a list of {@link TemplateDto}.
     */
    @GET
    @Path("/api/templates")
    @Produces(MediaType.APPLICATION_JSON)
    List<TemplateDto> listTemplates();

    /**
     * GET /api/templates/{id} — get a single template by ID.
     * Returns 200 with the matching {@link TemplateDto}.
     */
    @GET
    @Path("/api/templates/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    TemplateDto getTemplate(@PathParam("id") Long id);

    /**
     * PUT /api/templates/{id} — replace template content (multipart).
     * Returns 200 with the updated {@link TemplateDto}.
     */
    @PUT
    @Path("/api/templates/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    TemplateDto updateTemplate(@PathParam("id") Long id, @RestForm String description, @RestForm String content);

    /**
     * DELETE /api/templates/{id} — delete a template.
     * Returns 204 No Content.
     */
    @DELETE
    @Path("/api/templates/{id}")
    void deleteTemplate(@PathParam("id") Long id);

    // -------------------------------------------------------------------------
    // Document endpoints — /api/documents
    // -------------------------------------------------------------------------

    /**
     * POST /api/documents — generate a PDF from form fields.
     * Returns 201 with the created {@link DocumentDto}.
     */
    @POST
    @Path("/api/documents")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    DocumentDto generateDocument(@RestForm Long templateId,
                                 @RestForm String name,
                                 @RestForm String description,
                                 @RestForm String jsonOriginString,
                                 @RestForm String documentHtml);

    /**
     * GET /api/documents — list all document metadata.
     * Returns 200 with a list of {@link DocumentDto}.
     */
    @GET
    @Path("/api/documents")
    @Produces(MediaType.APPLICATION_JSON)
    List<DocumentDto> listDocuments();

    /**
     * GET /api/documents/{id} — download the PDF binary.
     * Returns 200 with {@code Content-Type: application/pdf}.
     */
    @GET
    @Path("/api/documents/{id}")
    @Produces("application/pdf")
    Response getPdf(@PathParam("id") Long id);

    /**
     * PUT /api/documents/{id} — update a document with new data.
     * Returns 200 with the updated {@link DocumentDto}.
     */
    @PUT
    @Path("/api/documents/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    DocumentDto updateDocument(@PathParam("id") Long id,
                               @RestForm String jsonOriginString,
                               @RestForm String documentHtml);

    /**
     * DELETE /api/documents/{id} — delete a document.
     * Returns 204 No Content.
     */
    @DELETE
    @Path("/api/documents/{id}")
    void deleteDocument(@PathParam("id") Long id);
}
