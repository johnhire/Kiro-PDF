package com.pdfman.resource;

import com.pdfman.dto.DocumentDto;
import com.pdfman.dto.GeneratePdfRequest;
import com.pdfman.service.DocumentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.util.List;

@Path("/api/documents")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {

    @Inject
    DocumentService documentService;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response generate(@RestForm Long templateId,
                             @RestForm String name,
                             @RestForm String description,
                             @RestForm String jsonOriginString,
                             @RestForm String documentHtml) {
        if (templateId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity("{\"error\": \"Field 'templateId' is required\"}")
                    .build();
        }
        DocumentDto dto = documentService.generate(templateId, name, description, jsonOriginString, documentHtml);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @GET
    public List<DocumentDto> listAll() {
        return documentService.listAll();
    }

    @GET
    @Path("/{id}")
    @Produces("application/pdf")
    public Response getPdf(@PathParam("id") Long id) {
        byte[] pdfBytes = documentService.getPdfBytes(id);
        return Response.ok(pdfBytes, "application/pdf").build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public DocumentDto update(@PathParam("id") Long id,
                              @RestForm String jsonOriginString,
                              @RestForm String documentHtml) {
        System.out.println("=== DocumentResource.update() called ===");
        System.out.println("id: " + id);
        System.out.println("jsonOriginString: " + jsonOriginString);
        System.out.println("documentHtml length: " + (documentHtml != null ? documentHtml.length() : "null"));
        return documentService.update(id, jsonOriginString, documentHtml);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        documentService.delete(id);
        return Response.noContent().build();
    }
}
