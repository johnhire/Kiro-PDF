package com.pdfman.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class StorageExceptionMapper implements ExceptionMapper<StorageException> {

    @Override
    public Response toResponse(StorageException exception) {
        return Response.status(503)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"error\": \"" + escape(exception.getMessage()) + "\"}")
                .build();
    }

    private String escape(String msg) {
        return msg == null ? "" : msg.replace("\"", "\\\"");
    }
}
