package org.acme.discord.api.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.exception.GuildNotEmptyException;

/**
 * Maps a non-empty guild deletion attempt to a client-safe JSON response.
 */
@Provider
public class GuildNotEmptyMapper implements ExceptionMapper<GuildNotEmptyException> {

    @Override
    public Response toResponse(GuildNotEmptyException exception) {
        return ErrorResponses.of(
                Response.Status.CONFLICT,
                "GUILD_NOT_EMPTY",
                exception.getMessage());
    }
}
