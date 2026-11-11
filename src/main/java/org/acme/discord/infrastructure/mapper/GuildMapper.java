package org.acme.discord.infrastructure.mapper;

import org.acme.discord.application.dto.GuildDTO;
import org.acme.discord.application.dto.usecases.CreateGuildRequest;
import org.acme.discord.application.dto.usecases.UpdateGuildRequest;
import org.acme.discord.domain.Guild;
import org.acme.discord.infrastructure.persistence.ChannelEntity;
import org.acme.discord.infrastructure.persistence.GuildEntity;
import org.acme.discord.infrastructure.persistence.RoleEntity;
import org.acme.discord.infrastructure.persistence.UserEntity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

/**
 * Maps guilds between API, domain, and persistence representations.
 */
public final class GuildMapper {

    private GuildMapper() {
        // Prevent instantiation of utility class.
    }

    /**
     * Maps a persistence entity to a response DTO.
     *
     * @param entity persisted guild
     * @return API response containing relation identifiers only
     */
    public static GuildDTO toDto(GuildEntity entity) {
        Guild domain = toDomain(entity);
        return new GuildDTO(
                domain.id(),
                domain.name(),
                domain.description(),
                domain.createdAt(),
                entity.getRoles().stream()
                        .map(RoleEntity::getId)
                        .collect(Collectors.toUnmodifiableSet()),
                entity.getChannels().stream()
                        .map(ChannelEntity::getId)
                        .collect(Collectors.toUnmodifiableSet()),
                entity.getMembers().stream()
                        .map(UserEntity::getId)
                        .collect(Collectors.toUnmodifiableSet()));
    }

    /**
     * Maps a persistence entity to the framework-independent domain model.
     *
     * @param entity persisted guild
     * @return domain guild
     */
    public static Guild toDomain(GuildEntity entity) {
        return new Guild(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt());
    }

    /**
     * Creates a new persistence entity using the server clock.
     *
     * @param request validated creation request
     * @return new guild entity
     */
    public static GuildEntity toEntity(CreateGuildRequest request) {
        return toEntity(request, Clock.systemUTC());
    }

    /**
     * Creates a new persistence entity using an explicit server clock.
     *
     * @param request validated creation request
     * @param clock server clock used to generate the creation date
     * @return new guild entity
     */
    public static GuildEntity toEntity(CreateGuildRequest request, Clock clock) {
        Guild domain = new Guild(
                null,
                request.name(),
                request.description(),
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        return new GuildEntity(domain.name(), domain.description(), domain.createdAt());
    }

    /**
     * Applies editable fields while preserving the server-generated identifier,
     * creation date, and relationships.
     *
     * @param request validated full update request
     * @param entity guild entity to update
     * @return the updated entity
     */
    public static GuildEntity updateEntity(UpdateGuildRequest request, GuildEntity entity) {
        Guild domain = new Guild(
                entity.getId(),
                request.name(),
                request.description(),
                entity.getCreatedAt());
        entity.setName(domain.name());
        entity.setDescription(domain.description());
        return entity;
    }
}
