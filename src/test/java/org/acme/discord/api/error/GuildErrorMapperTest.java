package org.acme.discord.api.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.discord.application.dto.ErrorDTO;
import org.acme.discord.application.exception.GuildNotEmptyException;
import org.acme.discord.application.exception.GuildNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GuildErrorMapperTest {

    @Test
    void shouldReturnGuildSpecificClientSafeJsonErrorsInUtc() {
        LocalDateTime before = LocalDateTime.now(Clock.systemUTC()).minusSeconds(1);

        assertError(new GuildNotFoundMapper().toResponse(new GuildNotFoundException()),
                404, "GUILD_NOT_FOUND", "Guild not found", before);
        assertError(new GuildNotEmptyMapper().toResponse(new GuildNotEmptyException()),
                409, "GUILD_NOT_EMPTY", "Guild must be empty before deletion", before);
    }

    private static void assertError(
            Response response,
            int expectedStatus,
            String expectedCode,
            String expectedMessage,
            LocalDateTime before) {
        ErrorDTO error = (ErrorDTO) response.getEntity();
        assertEquals(expectedStatus, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertEquals(expectedCode, error.code());
        assertEquals(expectedMessage, error.message());
        assertFalse(error.timestamp().isBefore(before));
        assertFalse(error.timestamp().isAfter(LocalDateTime.now(Clock.systemUTC()).plusSeconds(1)));
    }
}
