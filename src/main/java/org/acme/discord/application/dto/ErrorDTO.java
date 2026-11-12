package org.acme.discord.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Uniform API error representation.
 *
 * @param code stable error identifier
 * @param message client-safe error description
 * @param timestamp error creation date interpreted in UTC
 */
@Schema(description = "Uniform API error response")
public record ErrorDTO(String code, String message, LocalDateTime timestamp) {

    /**
     * Creates an error response using the server UTC clock.
     *
     * @param code stable error identifier
     * @param message client-safe error description
     * @return timestamped error response
     */
    public static ErrorDTO of(String code, String message) {
        return new ErrorDTO(code, message, LocalDateTime.now(Clock.systemUTC()));
    }
}
