package org.acme.discord.api.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.discord.application.dto.ErrorDTO;
import org.acme.discord.application.exception.RoleInUseException;
import org.acme.discord.application.exception.RoleNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RoleErrorMapperTest {

    @Test
    void shouldReturnRoleSpecificClientSafeJsonErrorsInUtc() {
        LocalDateTime before = LocalDateTime.now(Clock.systemUTC()).minusSeconds(1);

        assertError(new RoleNotFoundMapper().toResponse(new RoleNotFoundException()),
                404, "ROLE_NOT_FOUND", "Role not found", before);
        assertError(new RoleInUseMapper().toResponse(new RoleInUseException()),
                409, "ROLE_IN_USE", "Role is assigned to one or more users", before);
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
