package org.acme.discord.api.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps {@link IllegalStateException} to an HTTP 409 Conflict response.
 * A service can throw this standard Java exception when a business rule is
 * violated.
 */
@Provider
public class ConflictMapper implements ExceptionMapper<IllegalStateException> {
    @Override
    public Response toResponse(IllegalStateException ex) {
        return Response.status(Response.Status.CONFLICT)
                .entity(ex.getMessage()) // Send the business rule violation message to the client
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
