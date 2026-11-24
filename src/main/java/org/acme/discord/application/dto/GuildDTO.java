package org.acme.discord.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Public API representation of a guild.
 */
@Schema(description = "Guild representation for public API responses")
public record GuildDTO(
        @Schema(description = "Server-generated guild identifier", format = "uuid", readOnly = true,
                example = "00000000-0000-4000-8000-000000000201")
        UUID id,
        @Schema(description = "Guild name", example = "Demo Guild")
        String name,
        @Schema(description = "Optional guild description", example = "Guild used for API demonstrations")
        String description,
        @Schema(description = "UTC date and time when the guild was created", format = "date-time",
                readOnly = true, example = "2026-01-15T10:35:00")
        LocalDateTime createdAt,
        @Schema(description = "Identifiers of roles owned by the guild", readOnly = true)
        Set<UUID> roleIds,
        @Schema(description = "Identifiers of channels owned by the guild", readOnly = true)
        Set<UUID> channelIds,
        @Schema(description = "Identifiers of guild members", readOnly = true)
        Set<UUID> memberIds) {
}
