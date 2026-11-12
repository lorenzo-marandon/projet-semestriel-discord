package org.acme.discord.api.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.dto.ErrorDTO;
import org.acme.discord.application.exception.GuildNotEmptyException;

/**
 * Maps a non-empty guild deletion attempt to a client-safe JSON response.
 */
@Provider
public class GuildNotEmptyMapper implements ExceptionMapper<GuildNotEmptyException> {

    @Override
    public Response toResponse(GuildNotEmptyException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(ErrorDTO.of("GUILD_NOT_EMPTY", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
