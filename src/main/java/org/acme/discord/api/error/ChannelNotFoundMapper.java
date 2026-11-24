package org.acme.discord.api.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.exception.ChannelNotFoundException;

/**
 * Maps a missing channel to a client-safe JSON response.
 */
@Provider
public class ChannelNotFoundMapper implements ExceptionMapper<ChannelNotFoundException> {

    @Override
    public Response toResponse(ChannelNotFoundException exception) {
        return ErrorResponses.of(
                Response.Status.NOT_FOUND,
                "CHANNEL_NOT_FOUND",
                exception.getMessage());
    }
}
