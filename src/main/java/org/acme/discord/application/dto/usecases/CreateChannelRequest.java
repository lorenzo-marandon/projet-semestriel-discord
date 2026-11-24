package org.acme.discord.application.dto.usecases;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.acme.discord.application.json.StrictChannelTypeDeserializer;
import org.acme.discord.domain.ChannelType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

/**
 * Request payload used to create a channel in a guild.
 */
@Schema(description = "Request object to create a new channel")
public record CreateChannelRequest(
        @NotBlank(message = "Channel name is required")
        @Schema(description = "Channel name", example = "general")
        String name,

        @NotNull(message = "Channel type is required")
        @JsonDeserialize(using = StrictChannelTypeDeserializer.class)
        @Schema(description = "Channel type", enumeration = {"TEXT", "VOICE"}, example = "TEXT")
        ChannelType type,

        @NotNull(message = "Channel position is required")
        @PositiveOrZero(message = "Channel position cannot be negative")
        @Schema(description = "Non-negative display position", example = "0", minimum = "0")
        Integer position,

        @NotNull(message = "Channel guild is required")
        @Schema(description = "Identifier of the guild that will own the channel", format = "uuid",
                example = "00000000-0000-4000-8000-000000000201")
        UUID guildId) {
}
