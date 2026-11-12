package org.acme.discord.api.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.discord.application.dto.ErrorDTO;
import org.acme.discord.application.exception.ChannelNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ChannelErrorMapperTest {

    @Test
    void shouldReturnChannelSpecificClientSafeJsonErrorInUtc() {
        LocalDateTime before = LocalDateTime.now(Clock.systemUTC()).minusSeconds(1);

        Response response = new ChannelNotFoundMapper().toResponse(new ChannelNotFoundException());
        ErrorDTO error = (ErrorDTO) response.getEntity();

        assertEquals(404, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertEquals("CHANNEL_NOT_FOUND", error.code());
        assertEquals("Channel not found", error.message());
        assertFalse(error.timestamp().isBefore(before));
        assertFalse(error.timestamp().isAfter(LocalDateTime.now(Clock.systemUTC()).plusSeconds(1)));
    }
}
