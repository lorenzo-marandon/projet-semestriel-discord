package org.acme.discord.api.error;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.discord.application.dto.ErrorDTO;
import org.acme.discord.application.exception.UserNotFoundException;
import org.acme.discord.application.exception.UsernameConflictException;
import org.acme.discord.application.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorMapperTest {

    @Test
    void shouldReturnUniformClientSafeJsonErrors() {
        LocalDateTime before = LocalDateTime.now(Clock.systemUTC()).minusSeconds(1);
        assertError(new DomainExceptionMapper().toResponse(new InvalidRequestException("Invalid value")),
                400, "INVALID_REQUEST", "Invalid value");
        assertError(new NotFoundMapper().toResponse(new UserNotFoundException()),
                404, "USER_NOT_FOUND", "User not found");
        assertError(new ConflictMapper().toResponse(new UsernameConflictException()),
                409, "USERNAME_ALREADY_EXISTS", "Username is already in use");

        Response internal = new GlobalExceptionMapper().toResponse(
                new RuntimeException("SQL password=secret"));
        ErrorDTO error = (ErrorDTO) internal.getEntity();
        assertEquals(500, internal.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, internal.getMediaType());
        assertEquals("INTERNAL_SERVER_ERROR", error.code());
        assertFalse(error.message().contains("SQL"));
        assertFalse(error.message().contains("secret"));
        assertTrue(error.violations().isEmpty());
        assertFalse(error.timestamp().isBefore(before));
        assertFalse(error.timestamp().isAfter(LocalDateTime.now(Clock.systemUTC()).plusSeconds(1)));
    }

    @Test
    void shouldMapJsonFailuresAndPreserveFrameworkResponses() {
        assertError(new BadRequestMapper().toResponse(new BadRequestException()),
                400, "INVALID_JSON", "Invalid JSON structure");
        assertError(new JacksonMapper().toResponse(
                        MismatchedInputException.from(null, String.class, "technical detail")),
                400, "INVALID_JSON", "Invalid JSON structure");
        assertError(new JsonParseMapper().toResponse(new JsonParseException(null, "technical detail")),
                400, "INVALID_JSON", "Invalid JSON structure");

        Response methodNotAllowed = Response.status(405).header("Allow", "GET").build();
        Response unsupportedMediaType = Response.status(415).header("Accept", "application/json").build();
        GlobalExceptionMapper mapper = new GlobalExceptionMapper();

        assertSame(methodNotAllowed, mapper.toResponse(new WebApplicationException(methodNotAllowed)));
        assertEquals("GET", methodNotAllowed.getHeaderString("Allow"));
        assertSame(unsupportedMediaType, mapper.toResponse(new WebApplicationException(unsupportedMediaType)));
        assertEquals("application/json", unsupportedMediaType.getHeaderString("Accept"));
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
        assertTrue(error.violations().isEmpty());
        assertFalse(error.timestamp() == null);
    }
}
