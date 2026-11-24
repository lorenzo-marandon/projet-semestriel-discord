package org.acme.discord.application.dto.usecases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/**
 * Request payload used to create a role in a guild.
 */
@Schema(description = "Request object to create a new role")
public record CreateRoleRequest(
        @NotBlank(message = "Role name is required")
        @Schema(description = "Role name", example = "Moderator")
        String name,

        @NotBlank(message = "Role color is required")
        @Schema(description = "Role color represented as a hexadecimal RGB value", example = "#5865F2")
        String color,

        @NotNull(message = "Role position is required")
        @PositiveOrZero(message = "Role position cannot be negative")
        @Schema(description = "Non-negative display position", example = "1", minimum = "0")
        Integer position,

        @NotNull(message = "Role guild is required")
        @Schema(description = "Identifier of the guild that will own the role", format = "uuid",
                example = "00000000-0000-4000-8000-000000000201")
        UUID guildId) {
}
