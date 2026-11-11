package org.acme.discord.application.dto;

import org.acme.discord.domain.ChannelType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/**
 * Public API representation of a channel.
 */
@Schema(description = "Channel representation for public API responses")
public record ChannelDTO(
        UUID id,
        String name,
        ChannelType type,
        Integer position,
        UUID guildId) {
}
