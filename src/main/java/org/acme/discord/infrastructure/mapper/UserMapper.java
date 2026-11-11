package org.acme.discord.infrastructure.mapper;

import org.acme.discord.application.dto.UserDTO;
import org.acme.discord.application.dto.usecases.CreateUserRequest;
import org.acme.discord.application.dto.usecases.UpdateUserRequest;
import org.acme.discord.domain.User;
import org.acme.discord.infrastructure.persistence.GuildEntity;
import org.acme.discord.infrastructure.persistence.RoleEntity;
import org.acme.discord.infrastructure.persistence.UserEntity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

/**
 * Maps users between API, domain, and persistence representations.
 */
public final class UserMapper {

    private UserMapper() {
        // Prevent instantiation of utility class.
    }

    /**
     * Maps a persistence entity to a response DTO.
     *
     * @param entity persisted user
     * @return API response containing relation identifiers only
     */
    public static UserDTO toDto(UserEntity entity) {
        User domain = toDomain(entity);
        return new UserDTO(
                domain.id(),
                domain.username(),
                domain.displayName(),
                domain.joinedAt(),
                entity.getGuilds().stream()
                        .map(GuildEntity::getId)
                        .collect(Collectors.toUnmodifiableSet()),
                entity.getRoles().stream()
                        .map(RoleEntity::getId)
                        .collect(Collectors.toUnmodifiableSet()));
    }

    /**
     * Maps a persistence entity to the framework-independent domain model.
     *
     * @param entity persisted user
     * @return domain user
     */
    public static User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getDisplayName(),
                entity.getJoinedAt());
    }

    /**
     * Creates a new persistence entity using the server clock.
     *
     * @param request validated creation request
     * @return new user entity
     */
    public static UserEntity toEntity(CreateUserRequest request) {
        return toEntity(request, Clock.systemUTC());
    }

    /**
     * Creates a new persistence entity using an explicit server clock.
     *
     * @param request validated creation request
     * @param clock server clock used to generate the join date
     * @return new user entity
     */
    public static UserEntity toEntity(CreateUserRequest request, Clock clock) {
        User domain = new User(
                null,
                request.username(),
                request.displayName(),
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        return new UserEntity(domain.username(), domain.displayName(), domain.joinedAt());
    }

    /**
     * Applies editable fields while preserving the server-generated identifier,
     * join date, and relationships.
     *
     * @param request validated full update request
     * @param entity user entity to update
     * @return the updated entity
     */
    public static UserEntity updateEntity(UpdateUserRequest request, UserEntity entity) {
        User domain = new User(
                entity.getId(),
                request.username(),
                request.displayName(),
                entity.getJoinedAt());
        entity.setUsername(domain.username());
        entity.setDisplayName(domain.displayName());
        return entity;
    }
}
