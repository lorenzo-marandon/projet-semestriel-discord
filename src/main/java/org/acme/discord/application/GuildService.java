package org.acme.discord.application;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.acme.discord.application.dto.GuildDTO;
import org.acme.discord.application.dto.PageDTO;
import org.acme.discord.application.dto.usecases.CreateGuildRequest;
import org.acme.discord.application.dto.usecases.UpdateGuildRequest;
import org.acme.discord.application.exception.GuildNotEmptyException;
import org.acme.discord.application.exception.GuildNotFoundException;
import org.acme.discord.infrastructure.mapper.GuildMapper;
import org.acme.discord.infrastructure.persistence.GuildEntity;
import org.acme.discord.infrastructure.persistence.GuildRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Application service implementing Guild CRUD use cases.
 */
@ApplicationScoped
public class GuildService {

    private static final String FOREIGN_KEY_VIOLATION_SQL_STATE = "23503";

    private final GuildRepository guildRepository;

    public GuildService(GuildRepository guildRepository) {
        this.guildRepository = guildRepository;
    }

    /**
     * Creates and persists a guild.
     *
     * @param request validated creation request
     * @return created guild
     */
    @Transactional
    public GuildDTO create(CreateGuildRequest request) {
        GuildEntity entity = GuildMapper.toEntity(request);
        guildRepository.persistAndFlush(entity);
        return GuildMapper.toDto(entity);
    }

    /**
     * Retrieves a guild by identifier.
     *
     * @param id guild identifier
     * @return matching guild
     */
    @Transactional
    public GuildDTO get(UUID id) {
        return GuildMapper.toDto(findEntity(id));
    }

    /**
     * Lists guilds using stable database ordering and pagination.
     *
     * @param page zero-based page index
     * @param size page size
     * @return paginated guilds
     */
    @Transactional
    public PageDTO<GuildDTO> list(int page, int size) {
        validatePagination(page, size);
        PanacheQuery<GuildEntity> query = guildRepository.findAll(Sort.ascending("createdAt", "id"));
        long totalItems = query.count();
        List<GuildDTO> items = query.page(Page.of(page, size)).list().stream()
                .map(GuildMapper::toDto)
                .toList();
        int totalPages = totalItems == 0 ? 0 : (int) ((totalItems + size - 1) / size);
        return new PageDTO<>(items, page, size, totalItems, totalPages);
    }

    /**
     * Fully updates a guild's editable fields.
     *
     * @param id guild identifier
     * @param request validated update request
     * @return updated guild
     */
    @Transactional
    public GuildDTO update(UUID id, UpdateGuildRequest request) {
        GuildEntity entity = findEntity(id);
        GuildMapper.updateEntity(request, entity);
        guildRepository.flush();
        return GuildMapper.toDto(entity);
    }

    /**
     * Deletes an empty guild while protecting the aggregate from concurrent changes.
     *
     * @param id guild identifier
     */
    @Transactional
    public void delete(UUID id) {
        GuildEntity entity = guildRepository.findByIdForUpdate(id)
                .orElseThrow(GuildNotFoundException::new);
        if (guildRepository.hasMembers(id)
                || guildRepository.hasRoles(id)
                || guildRepository.hasChannels(id)) {
            throw new GuildNotEmptyException();
        }

        try {
            guildRepository.delete(entity);
            guildRepository.flush();
        } catch (PersistenceException exception) {
            if (isForeignKeyViolation(exception)) {
                throw new GuildNotEmptyException();
            }
            throw exception;
        }
    }

    private GuildEntity findEntity(UUID id) {
        return guildRepository.findByIdOptional(id)
                .orElseThrow(GuildNotFoundException::new);
    }

    private static void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }

    private static boolean isForeignKeyViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException
                    && FOREIGN_KEY_VIOLATION_SQL_STATE.equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
