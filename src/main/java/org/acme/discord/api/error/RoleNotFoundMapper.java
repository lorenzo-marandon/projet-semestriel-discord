package org.acme.discord.api.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.exception.RoleNotFoundException;

/**
 * Maps a missing role to a client-safe JSON response.
 */
@Provider
public class RoleNotFoundMapper implements ExceptionMapper<RoleNotFoundException> {

    @Override
    public Response toResponse(RoleNotFoundException exception) {
        return ErrorResponses.of(
                Response.Status.NOT_FOUND,
                "ROLE_NOT_FOUND",
                exception.getMessage());
    }
}
