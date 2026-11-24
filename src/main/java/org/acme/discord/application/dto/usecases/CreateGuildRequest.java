package org.acme.discord.application.dto.usecases;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request payload used to create a guild.
 */
@Schema(description = "Request object to create a new guild")
public record CreateGuildRequest(
        @NotBlank(message = "Guild name is required")
        @Schema(description = "Guild name", example = "Demo Guild")
        String name,

        @Schema(description = "Optional guild description", example = "Guild used for API demonstrations")
        String description) {
}
