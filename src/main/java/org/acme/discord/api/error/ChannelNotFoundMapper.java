package org.acme.discord.api.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.dto.ErrorDTO;
import org.acme.discord.application.exception.ChannelNotFoundException;

/**
 * Maps a missing channel to a client-safe JSON response.
 */
@Provider
public class ChannelNotFoundMapper implements ExceptionMapper<ChannelNotFoundException> {

    @Override
    public Response toResponse(ChannelNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorDTO.of("CHANNEL_NOT_FOUND", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
