package org.acme.discord.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/**
 * Public API representation of a role.
 */
@Schema(description = "Role representation for public API responses")
public record RoleDTO(
        @Schema(description = "Server-generated role identifier", format = "uuid", readOnly = true,
                example = "00000000-0000-4000-8000-000000000301")
        UUID id,
        @Schema(description = "Role name", example = "Moderator")
        String name,
        @Schema(description = "Role color represented as a hexadecimal RGB value", example = "#5865F2")
        String color,
        @Schema(description = "Non-negative display position", example = "1", minimum = "0")
        Integer position,
        @Schema(description = "Identifier of the guild that owns the role", format = "uuid", readOnly = true,
                example = "00000000-0000-4000-8000-000000000201")
        UUID guildId) {
}
