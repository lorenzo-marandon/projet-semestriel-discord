package org.acme.discord.application;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.acme.discord.application.dto.PageDTO;
import org.acme.discord.application.dto.RoleDTO;
import org.acme.discord.application.dto.usecases.CreateRoleRequest;
import org.acme.discord.application.dto.usecases.UpdateRoleRequest;
import org.acme.discord.application.exception.GuildNotFoundException;
import org.acme.discord.application.exception.RoleInUseException;
import org.acme.discord.application.exception.RoleNotFoundException;
import org.acme.discord.infrastructure.mapper.RoleMapper;
import org.acme.discord.infrastructure.persistence.GuildEntity;
import org.acme.discord.infrastructure.persistence.GuildRepository;
import org.acme.discord.infrastructure.persistence.RoleEntity;
import org.acme.discord.infrastructure.persistence.RoleRepository;

import java.util.List;
import java.util.UUID;

/**
 * Application service implementing Role CRUD use cases.
 */
@ApplicationScoped
public class RoleService {

    private final RoleRepository roleRepository;
    private final GuildRepository guildRepository;

    public RoleService(RoleRepository roleRepository, GuildRepository guildRepository) {
        this.roleRepository = roleRepository;
        this.guildRepository = guildRepository;
    }

    /**
     * Creates and persists a role in an existing guild.
     *
     * @param request validated creation request
     * @return created role
     */
    @Transactional
    public RoleDTO create(CreateRoleRequest request) {
        GuildEntity guild = guildRepository.findByIdForRead(request.guildId())
                .orElseThrow(GuildNotFoundException::new);
        RoleEntity entity = RoleMapper.toEntity(request, guild);
        roleRepository.persistAndFlush(entity);
        return RoleMapper.toDto(entity);
    }

    /**
     * Retrieves a role by identifier.
     *
     * @param id role identifier
     * @return matching role
     */
    @Transactional
    public RoleDTO get(UUID id) {
        return RoleMapper.toDto(findEntity(id));
    }

    /**
     * Lists roles using stable database ordering and pagination.
     *
     * @param page zero-based page index
     * @param size page size
     * @return paginated roles
     */
    @Transactional
    public PageDTO<RoleDTO> list(int page, int size) {
        validatePagination(page, size);
        PanacheQuery<RoleEntity> query = roleRepository.findAll(Sort.ascending("position", "id"));
        long totalItems = query.count();
        List<RoleDTO> items = query.page(Page.of(page, size)).list().stream()
                .map(RoleMapper::toDto)
                .toList();
        int totalPages = totalItems == 0 ? 0 : (int) ((totalItems + size - 1) / size);
        return new PageDTO<>(items, page, size, totalItems, totalPages);
    }

    /**
     * Fully updates a role's editable fields while preserving its guild and assignments.
     *
     * @param id role identifier
     * @param request validated update request
     * @return updated role
     */
    @Transactional
    public RoleDTO update(UUID id, UpdateRoleRequest request) {
        RoleEntity entity = findEntity(id);
        RoleMapper.updateEntity(request, entity);
        roleRepository.flush();
        return RoleMapper.toDto(entity);
    }

    /**
     * Deletes an unassigned role while protecting it from concurrent changes.
     *
     * @param id role identifier
     */
    @Transactional
    public void delete(UUID id) {
        RoleEntity entity = roleRepository.findByIdForUpdate(id)
                .orElseThrow(RoleNotFoundException::new);
        if (roleRepository.hasUsers(id)) {
            throw new RoleInUseException();
        }

        try {
            roleRepository.delete(entity);
            roleRepository.flush();
        } catch (PersistenceException exception) {
            if (roleRepository.isUserRoleForeignKeyViolation(exception)) {
                throw new RoleInUseException();
            }
            throw exception;
        }
    }

    private RoleEntity findEntity(UUID id) {
        return roleRepository.findByIdOptional(id)
                .orElseThrow(RoleNotFoundException::new);
    }

    private static void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }
}
