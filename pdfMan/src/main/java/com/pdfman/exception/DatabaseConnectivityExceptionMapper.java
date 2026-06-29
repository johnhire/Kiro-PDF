package com.pdfman.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DatabaseConnectivityExceptionMapper implements ExceptionMapper<DatabaseConnectivityException> {

    @Override
    public Response toResponse(DatabaseConnectivityException exception) {
        return Response.status(503)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"error\": \"" + escape(exception.getMessage()) + "\"}")
                .build();
    }

    private String escape(String msg) {
        return msg == null ? "" : msg.replace("\"", "\\\"");
    }
}
