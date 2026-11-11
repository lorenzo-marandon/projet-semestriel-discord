package org.acme.discord.application.dto.usecases;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request payload used to create a guild.
 */
@Schema(description = "Request object to create a new guild")
public record CreateGuildRequest(
        @NotBlank(message = "Guild name is required")
        String name,

        String description) {
}
