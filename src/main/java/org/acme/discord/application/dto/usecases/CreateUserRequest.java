package org.acme.discord.application.dto.usecases;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request payload used to create a user.
 */
@Schema(description = "Request object to create a new user")
public record CreateUserRequest(
        @NotBlank(message = "Username is required")
        @Schema(description = "Unique username", example = "demo_user")
        String username,

        @NotBlank(message = "Display name is required")
        @Schema(description = "Public display name", example = "Demo User")
        String displayName) {
}
