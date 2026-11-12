package org.acme.discord.api.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.dto.ErrorDTO;
import java.util.NoSuchElementException;

/**
 * Maps {@link NoSuchElementException} to an HTTP 404 Not Found.
 * * Pedagogical Point: The Service returns an Optional or throws this
 * standard Java exception. This mapper bridges that "Pure Java" logic back to
 * REST.
 */
@Provider
public class NotFoundMapper implements ExceptionMapper<NoSuchElementException> {
    @Override
    public Response toResponse(NoSuchElementException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorDTO.of("USER_NOT_FOUND", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
