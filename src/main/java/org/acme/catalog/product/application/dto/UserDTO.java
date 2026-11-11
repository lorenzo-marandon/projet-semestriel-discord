package org.acme.catalog.product.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Public API representation of a user.
 */
@Schema(description = "User representation for public API responses")
public record UserDTO(
        UUID id,
        String username,
        String displayName,
        LocalDateTime joinedAt,
        Set<UUID> guildIds,
        Set<UUID> roleIds) {
}
