package org.acme.discord.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Uniform API error representation.
 *
 * @param code stable error identifier
 * @param message client-safe error description
 * @param timestamp error creation date interpreted in UTC
 * @param violations field validation details, empty for non-validation errors
 */
@Schema(description = "Uniform API error response")
public record ErrorDTO(
        String code,
        String message,
        LocalDateTime timestamp,
        List<ValidationViolationDTO> violations) {

    public ErrorDTO {
        violations = List.copyOf(violations);
    }

    /**
     * Creates an error response using the server UTC clock.
     *
     * @param code stable error identifier
     * @param message client-safe error description
     * @return timestamped error response
     */
    public static ErrorDTO of(String code, String message) {
        return new ErrorDTO(code, message, LocalDateTime.now(Clock.systemUTC()), List.of());
    }

    /**
     * Creates a request validation error using the server UTC clock.
     *
     * @param violations field validation details
     * @return timestamped validation error response
     */
    public static ErrorDTO validation(List<ValidationViolationDTO> violations) {
        return new ErrorDTO(
                "VALIDATION_ERROR",
                "Request validation failed",
                LocalDateTime.now(Clock.systemUTC()),
                violations);
    }
}
