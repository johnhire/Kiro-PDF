package com.pdfman.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DuplicateNameExceptionMapper implements ExceptionMapper<DuplicateNameException> {

    @Override
    public Response toResponse(DuplicateNameException exception) {
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"error\": \"" + escape(exception.getMessage()) + "\"}")
                .build();
    }

    private String escape(String msg) {
        return msg == null ? "" : msg.replace("\"", "\\\"");
    }
}
