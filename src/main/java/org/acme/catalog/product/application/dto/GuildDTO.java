package org.acme.catalog.product.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Public API representation of a guild.
 */
@Schema(description = "Guild representation for public API responses")
public record GuildDTO(
        UUID id,
        String name,
        String description,
        LocalDateTime createdAt,
        Set<UUID> roleIds,
        Set<UUID> channelIds,
        Set<UUID> memberIds) {
}
