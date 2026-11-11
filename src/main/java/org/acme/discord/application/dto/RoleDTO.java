package org.acme.discord.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/**
 * Public API representation of a role.
 */
@Schema(description = "Role representation for public API responses")
public record RoleDTO(
        UUID id,
        String name,
        String color,
        Integer position,
        UUID guildId) {
}
