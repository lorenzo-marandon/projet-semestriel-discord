package org.acme.discord.api.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.exception.GuildNotFoundException;

/**
 * Maps a missing guild to a client-safe JSON response.
 */
@Provider
public class GuildNotFoundMapper implements ExceptionMapper<GuildNotFoundException> {

    @Override
    public Response toResponse(GuildNotFoundException exception) {
        return ErrorResponses.of(
                Response.Status.NOT_FOUND,
                "GUILD_NOT_FOUND",
                exception.getMessage());
    }
}
