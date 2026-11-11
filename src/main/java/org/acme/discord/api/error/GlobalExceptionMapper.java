package org.acme.discord.api.error;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The "Final Safety Net" for the API.
 * * This mapper catches any Throwable that wasn't caught by more specific
 * mappers.
 * It fulfills Non-Functional Requirement (NFR-SEC-03): Prevention of technical
 * data leakage.
 */
@Provider
@Priority(Priorities.USER + 100) // Low priority: only runs if no specific mapper is found
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {

        // 1. Transparently pass through existing JAX-RS exceptions (like 405 Method Not
        // Allowed)
        // to preserve the web framework's native behavior.
        if (exception instanceof WebApplicationException webEx) {
            Response originalResponse = webEx.getResponse();
            if (originalResponse.getStatus() < 500) {
                return originalResponse;
            }
        }

        // 2. Catch-all for unexpected errors (NullPointer, SQL issues, etc.)
        // We log the real error for developers but hide details from the client.
        LOG.error("Unhandled error caught by Global Mapper:", exception);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("An unexpected error occurred. Please contact support.")
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
