package org.acme.catalog.product.infrastructure.mapper;

import org.acme.catalog.product.application.dto.RoleDTO;
import org.acme.catalog.product.application.dto.usecases.CreateRoleRequest;
import org.acme.catalog.product.application.dto.usecases.UpdateRoleRequest;
import org.acme.catalog.product.domain.Role;
import org.acme.catalog.product.infrastructure.persistence.GuildEntity;
import org.acme.catalog.product.infrastructure.persistence.RoleEntity;

import java.util.Objects;

/**
 * Maps roles between API, domain, and persistence representations.
 */
public final class RoleMapper {

    private RoleMapper() {
        // Prevent instantiation of utility class.
    }

    /**
     * Maps a persistence entity to a response DTO.
     *
     * @param entity persisted role
     * @return API response containing the guild identifier
     */
    public static RoleDTO toDto(RoleEntity entity) {
        Role domain = toDomain(entity);
        return new RoleDTO(
                domain.id(),
                domain.name(),
                domain.color(),
                domain.position(),
                entity.getGuild().getId());
    }

    /**
     * Maps a persistence entity to the framework-independent domain model.
     *
     * @param entity persisted role
     * @return domain role
     */
    public static Role toDomain(RoleEntity entity) {
        return new Role(
                entity.getId(),
                entity.getName(),
                entity.getColor(),
                entity.getPosition());
    }

    /**
     * Creates a role associated with the guild resolved by the application
     * service. This mapper performs no database lookup.
     *
     * @param request validated creation request
     * @param guild guild previously resolved by its identifier
     * @return new role entity
     * @throws IllegalArgumentException if the supplied guild does not match the request
     */
    public static RoleEntity toEntity(CreateRoleRequest request, GuildEntity guild) {
        requireMatchingGuild(request.guildId(), guild);
        Role domain = new Role(null, request.name(), request.color(), request.position());
        RoleEntity entity = new RoleEntity(domain.name(), domain.color(), domain.position());
        entity.setGuild(guild);
        return entity;
    }

    /**
     * Applies editable fields while preserving the identifier and guild.
     *
     * @param request validated full update request
     * @param entity role entity to update
     * @return the updated entity
     */
    public static RoleEntity updateEntity(UpdateRoleRequest request, RoleEntity entity) {
        Role domain = new Role(
                entity.getId(),
                request.name(),
                request.color(),
                request.position());
        entity.setName(domain.name());
        entity.setColor(domain.color());
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
