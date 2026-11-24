package org.acme.discord.application.dto;

import org.acme.discord.domain.ChannelType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/**
 * Public API representation of a channel.
 */
@Schema(description = "Channel representation for public API responses")
public record ChannelDTO(
        @Schema(description = "Server-generated channel identifier", format = "uuid", readOnly = true,
                example = "00000000-0000-4000-8000-000000000401")
        UUID id,
        @Schema(description = "Channel name", example = "general")
        String name,
        @Schema(description = "Channel type", enumeration = {"TEXT", "VOICE"}, example = "TEXT")
        ChannelType type,
        @Schema(description = "Non-negative display position", example = "0", minimum = "0")
        Integer position,
        @Schema(description = "Identifier of the guild that owns the channel", format = "uuid", readOnly = true,
                example = "00000000-0000-4000-8000-000000000201")
        UUID guildId) {
}
