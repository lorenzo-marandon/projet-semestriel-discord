package org.acme.discord.api.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.exception.RoleInUseException;

/**
 * Maps an assigned role deletion attempt to a client-safe JSON response.
 */
@Provider
public class RoleInUseMapper implements ExceptionMapper<RoleInUseException> {

    @Override
    public Response toResponse(RoleInUseException exception) {
        return ErrorResponses.of(
                Response.Status.CONFLICT,
                "ROLE_IN_USE",
                exception.getMessage());
    }
}
