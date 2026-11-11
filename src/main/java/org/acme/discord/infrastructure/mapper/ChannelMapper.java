package org.acme.discord.infrastructure.mapper;

import org.acme.discord.application.dto.ChannelDTO;
import org.acme.discord.application.dto.usecases.CreateChannelRequest;
import org.acme.discord.application.dto.usecases.UpdateChannelRequest;
import org.acme.discord.domain.Channel;
import org.acme.discord.infrastructure.persistence.ChannelEntity;
import org.acme.discord.infrastructure.persistence.GuildEntity;

import java.util.Objects;

/**
 * Maps channels between API, domain, and persistence representations.
 */
public final class ChannelMapper {

    private ChannelMapper() {
        // Prevent instantiation of utility class.
    }

    /**
     * Maps a persistence entity to a response DTO.
     *
     * @param entity persisted channel
     * @return API response containing the guild identifier
     */
    public static ChannelDTO toDto(ChannelEntity entity) {
        Channel domain = toDomain(entity);
        return new ChannelDTO(
                domain.id(),
                domain.name(),
                domain.type(),
                domain.position(),
                entity.getGuild().getId());
    }

    /**
     * Maps a persistence entity to the framework-independent domain model.
     *
     * @param entity persisted channel
     * @return domain channel
     */
    public static Channel toDomain(ChannelEntity entity) {
        return new Channel(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getPosition());
    }

    /**
     * Creates a channel associated with the guild resolved by the application
     * service. This mapper performs no database lookup.
     *
     * @param request validated creation request
     * @param guild guild previously resolved by its identifier
     * @return new channel entity
     * @throws IllegalArgumentException if the supplied guild does not match the request
     */
    public static ChannelEntity toEntity(CreateChannelRequest request, GuildEntity guild) {
        requireMatchingGuild(request.guildId(), guild);
        Channel domain = new Channel(null, request.name(), request.type(), request.position());
        ChannelEntity entity = new ChannelEntity(domain.name(), domain.type(), domain.position());
        entity.setGuild(guild);
        return entity;
    }

    /**
     * Applies editable fields while preserving the identifier and guild.
     *
     * @param request validated full update request
     * @param entity channel entity to update
     * @return the updated entity
     */
    public static ChannelEntity updateEntity(UpdateChannelRequest request, ChannelEntity entity) {
        Channel domain = new Channel(
                entity.getId(),
                request.name(),
                request.type(),
                request.position());
        entity.setName(domain.name());
        entity.setType(domain.type());
        entity.setPosition(domain.position());
        return entity;
    }

    private static void requireMatchingGuild(java.util.UUID guildId, GuildEntity guild) {
        Objects.requireNonNull(guild, "Guild is required");
        if (!guildId.equals(guild.getId())) {
            throw new IllegalArgumentException("Resolved guild does not match request guildId");
        }
    }
}
