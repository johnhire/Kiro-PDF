package com.pdfman.resource;

import com.pdfman.dto.TemplateDto;
import com.pdfman.service.TemplateService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.util.List;

@Path("/api/templates")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateResource {

    @Inject
    TemplateService templateService;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response create(@RestForm String templateType, @RestForm String description, @RestForm String content) {
        if (templateType == null || templateType.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity("{\"error\": \"Field 'templateType' is required\"}")
                    .build();
        }
        if (content == null || content.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity("{\"error\": \"Field 'content' is required\"}")
                    .build();
        }
        TemplateDto dto = templateService.create(templateType, description, content);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @GET
    public List<TemplateDto> listAll() {
        return templateService.listAll();
    }

    @GET
    @Path("/{id}")
    public TemplateDto findById(@PathParam("id") Long id) {
        return templateService.findById(id);
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public TemplateDto update(@PathParam("id") Long id, @RestForm String description, @RestForm String content) {
        return templateService.update(id, description, content);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        templateService.delete(id);
        return Response.noContent().build();
    }
}
