package org.acme.discord.api.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.exception.UserNotFoundException;

/**
 * Maps a missing user to an HTTP 404 Not Found response.
 */
@Provider
public class NotFoundMapper implements ExceptionMapper<UserNotFoundException> {
    @Override
    public Response toResponse(UserNotFoundException exception) {
        return ErrorResponses.of(
                Response.Status.NOT_FOUND,
                "USER_NOT_FOUND",
                exception.getMessage());
    }
}
