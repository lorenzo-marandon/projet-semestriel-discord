package org.acme.discord.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Client-safe description of one request validation failure.
 *
 * @param field invalid request field or parameter
 * @param message validation message
 */
@Schema(description = "Request validation violation")
public record ValidationViolationDTO(
        @Schema(description = "Invalid request field or parameter", example = "name")
        String field,
        @Schema(description = "Constraint violation message", example = "Guild name is required")
        String message) {
}
