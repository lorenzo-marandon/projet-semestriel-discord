package org.acme.catalog.product.application.dto.usecases;

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
        String name,

        @NotBlank(message = "Role color is required")
        String color,

        @NotNull(message = "Role position is required")
        @PositiveOrZero(message = "Role position cannot be negative")
        Integer position,

        @NotNull(message = "Role guild is required")
        UUID guildId) {
}
