package org.acme.discord.application.dto.usecases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.acme.discord.domain.ChannelType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request payload used to fully update a channel's editable fields.
 */
@Schema(description = "Request object to update an existing channel")
public record UpdateChannelRequest(
        @NotBlank(message = "Channel name is required")
        String name,

        @NotNull(message = "Channel type is required")
        ChannelType type,

        @NotNull(message = "Channel position is required")
        @PositiveOrZero(message = "Channel position cannot be negative")
        Integer position) {
}
