package org.acme.discord.api.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.dto.ErrorDTO;
import org.acme.discord.application.exception.RoleNotFoundException;

/**
 * Maps a missing role to a client-safe JSON response.
 */
@Provider
public class RoleNotFoundMapper implements ExceptionMapper<RoleNotFoundException> {

    @Override
    public Response toResponse(RoleNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorDTO.of("ROLE_NOT_FOUND", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
