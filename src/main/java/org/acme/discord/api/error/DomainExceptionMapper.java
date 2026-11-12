package org.acme.discord.api.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.dto.ErrorDTO;

/**
 * Maps {@link IllegalArgumentException} to an HTTP 400 Bad Request.
 * Handles domain validation failures and returns the validation message to the
 * client.
 */
@Provider
public class DomainExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorDTO.of("INVALID_REQUEST", exception.getMessage()))
                .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                .build();
    }
}
