package org.acme.discord.api.error;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps framework-level bad requests, including malformed request bodies, to JSON.
 */
@Provider
public class BadRequestMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        if (exception.getResponse().getStatus() != Response.Status.BAD_REQUEST.getStatusCode()) {
            return exception.getResponse();
        }
        return ErrorResponses.invalidJson();
    }
}
