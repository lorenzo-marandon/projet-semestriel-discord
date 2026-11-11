package org.acme.discord.application.dto.usecases;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request payload used to fully update a user's editable fields.
 */
@Schema(description = "Request object to update an existing user")
public record UpdateUserRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Display name is required")
        String displayName) {
}
