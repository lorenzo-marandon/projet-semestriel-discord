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
        @Schema(description = "Stable machine-readable error code",
                enumeration = {"VALIDATION_ERROR", "INVALID_JSON", "INVALID_REQUEST", "USER_NOT_FOUND",
                        "GUILD_NOT_FOUND", "ROLE_NOT_FOUND", "CHANNEL_NOT_FOUND", "USERNAME_ALREADY_EXISTS",
                        "USER_NOT_GUILD_MEMBER", "GUILD_NOT_EMPTY", "ROLE_IN_USE", "INTERNAL_SERVER_ERROR"},
                example = "VALIDATION_ERROR")
        String code,
        @Schema(description = "Client-safe error message", example = "Request validation failed")
        String message,
        @Schema(description = "UTC date and time when the error was produced", format = "date-time",
                example = "2026-01-15T10:45:00")
        LocalDateTime timestamp,
        @Schema(description = "Field violations; empty for errors unrelated to Bean Validation")
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
