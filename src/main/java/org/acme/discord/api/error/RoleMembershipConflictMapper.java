package org.acme.discord.api.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.dto.ErrorDTO;
import org.acme.discord.application.exception.RoleMembershipConflictException;

/**
 * Maps an incompatible role assignment to an HTTP 409 Conflict response.
 */
@Provider
public class RoleMembershipConflictMapper implements ExceptionMapper<RoleMembershipConflictException> {

    @Override
    public Response toResponse(RoleMembershipConflictException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(ErrorDTO.of("USER_NOT_GUILD_MEMBER", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
