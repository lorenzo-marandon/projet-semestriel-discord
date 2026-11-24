package org.acme.discord.api.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.discord.application.dto.ErrorDTO;
import org.acme.discord.application.dto.ValidationViolationDTO;

import java.util.List;

/**
 * Centralizes construction of client-safe JSON error responses.
 */
final class ErrorResponses {

    private ErrorResponses() {
        // Prevent instantiation of utility class.
    }

    static Response of(Response.Status status, String code, String message) {
        return Response.status(status)
                .entity(ErrorDTO.of(code, message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    static Response validation(List<ValidationViolationDTO> violations) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorDTO.validation(violations))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    static Response invalidJson() {
        return of(Response.Status.BAD_REQUEST, "INVALID_JSON", "Invalid JSON structure");
    }

    static Response internalServerError() {
        return of(
                Response.Status.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please contact support.");
    }
}
