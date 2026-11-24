package org.acme.discord.api.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.exception.InvalidRequestException;

/**
 * Maps invalid client parameters to an HTTP 400 Bad Request.
 */
@Provider
public class DomainExceptionMapper implements ExceptionMapper<InvalidRequestException> {
    @Override
    public Response toResponse(InvalidRequestException exception) {
        return ErrorResponses.of(
                Response.Status.BAD_REQUEST,
                "INVALID_REQUEST",
                exception.getMessage());
    }
}
