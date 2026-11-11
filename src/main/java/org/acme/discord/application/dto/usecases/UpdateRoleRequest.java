package org.acme.discord.application.dto.usecases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request payload used to fully update a role's editable fields.
 */
@Schema(description = "Request object to update an existing role")
public record UpdateRoleRequest(
        @NotBlank(message = "Role name is required")
        String name,

        @NotBlank(message = "Role color is required")
        String color,

        @NotNull(message = "Role position is required")
        @PositiveOrZero(message = "Role position cannot be negative")
        Integer position) {
}
