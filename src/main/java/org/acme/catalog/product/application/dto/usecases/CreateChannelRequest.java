package org.acme.catalog.product.application.dto.usecases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.acme.catalog.product.domain.ChannelType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/**
 * Request payload used to create a channel in a guild.
 */
@Schema(description = "Request object to create a new channel")
public record CreateChannelRequest(
        @NotBlank(message = "Channel name is required")
        String name,

        @NotNull(message = "Channel type is required")
        ChannelType type,

        @NotNull(message = "Channel position is required")
        @PositiveOrZero(message = "Channel position cannot be negative")
        Integer position,

        @NotNull(message = "Channel guild is required")
        UUID guildId) {
}
