package org.acme.discord.api.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.discord.application.dto.ErrorDTO;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.time.Clock;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ErrorMapperTest {

    @Test
    void shouldReturnUniformClientSafeJsonErrors() {
        LocalDateTime before = LocalDateTime.now(Clock.systemUTC()).minusSeconds(1);
        assertError(new DomainExceptionMapper().toResponse(new IllegalArgumentException("Invalid value")),
                400, "INVALID_REQUEST", "Invalid value");
        assertError(new NotFoundMapper().toResponse(new NoSuchElementException("User not found")),
                404, "USER_NOT_FOUND", "User not found");
        assertError(new ConflictMapper().toResponse(new IllegalStateException("Username is already in use")),
                409, "USERNAME_ALREADY_EXISTS", "Username is already in use");

        Response internal = new GlobalExceptionMapper().toResponse(
                new RuntimeException("SQL password=secret"));
        ErrorDTO error = (ErrorDTO) internal.getEntity();
        assertEquals(500, internal.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, internal.getMediaType());
        assertEquals("INTERNAL_SERVER_ERROR", error.code());
        assertFalse(error.message().contains("SQL"));
        assertFalse(error.message().contains("secret"));
        assertFalse(error.timestamp().isBefore(before));
        assertFalse(error.timestamp().isAfter(LocalDateTime.now(Clock.systemUTC()).plusSeconds(1)));
    }

    private static void assertError(
            Response response,
            int expectedStatus,
            String expectedCode,
            String expectedMessage) {
        ErrorDTO error = (ErrorDTO) response.getEntity();
        assertEquals(expectedStatus, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertEquals(expectedCode, error.code());
        assertEquals(expectedMessage, error.message());
        assertFalse(error.timestamp() == null);
    }
}
