package org.acme.discord.api.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.exception.RoleMembershipConflictException;

/**
 * Maps an incompatible role assignment to an HTTP 409 Conflict response.
 */
@Provider
public class RoleMembershipConflictMapper implements ExceptionMapper<RoleMembershipConflictException> {

    @Override
    public Response toResponse(RoleMembershipConflictException exception) {
        return ErrorResponses.of(
                Response.Status.CONFLICT,
                "USER_NOT_GUILD_MEMBER",
                exception.getMessage());
    }
}
