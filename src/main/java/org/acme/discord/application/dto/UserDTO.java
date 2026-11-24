package org.acme.discord.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Public API representation of a user.
 */
@Schema(description = "User representation for public API responses")
public record UserDTO(
        @Schema(description = "Server-generated user identifier", format = "uuid", readOnly = true,
                example = "00000000-0000-4000-8000-000000000101")
        UUID id,
        @Schema(description = "Unique username", example = "demo_user")
        String username,
        @Schema(description = "Public display name", example = "Demo User")
        String displayName,
        @Schema(description = "UTC date and time when the user was created", format = "date-time",
                readOnly = true, example = "2026-01-15T10:30:00")
        LocalDateTime joinedAt,
        @Schema(description = "Identifiers of guilds the user belongs to", readOnly = true)
        Set<UUID> guildIds,
        @Schema(description = "Identifiers of roles assigned to the user", readOnly = true)
        Set<UUID> roleIds) {
}
