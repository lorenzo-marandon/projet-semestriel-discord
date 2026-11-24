package org.acme.discord.application.dto.usecases;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request payload used to fully update a guild's editable fields.
 */
@Schema(description = "Request object to update an existing guild")
public record UpdateGuildRequest(
        @NotBlank(message = "Guild name is required")
        @Schema(description = "Guild name", example = "Updated Demo Guild")
        String name,

        @Schema(description = "Optional guild description", example = "Updated demonstration guild")
        String description) {
}
