package org.acme.discord.api.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Handles JSON parsing errors.
 * Prevents the client from seeing raw Jackson serialization stack traces
 * if they send a malformed JSON body (e.g., a missing bracket).
 */
@Provider
public class JacksonMapper implements ExceptionMapper<com.fasterxml.jackson.core.JsonProcessingException> {
    @Override
    public Response toResponse(com.fasterxml.jackson.core.JsonProcessingException ex) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid JSON structure")
                .build();
    }
}
