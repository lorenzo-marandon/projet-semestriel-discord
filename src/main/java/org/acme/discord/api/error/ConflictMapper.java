package org.acme.discord.api.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.exception.UsernameConflictException;

/**
 * Maps a duplicate username to an HTTP 409 Conflict response.
 */
@Provider
public class ConflictMapper implements ExceptionMapper<UsernameConflictException> {
    @Override
    public Response toResponse(UsernameConflictException exception) {
        return ErrorResponses.of(
                Response.Status.CONFLICT,
                "USERNAME_ALREADY_EXISTS",
                exception.getMessage());
    }
}
