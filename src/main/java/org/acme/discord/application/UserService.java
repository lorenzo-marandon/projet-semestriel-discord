package org.acme.discord.application;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.acme.discord.application.dto.PageDTO;
import org.acme.discord.application.dto.UserDTO;
import org.acme.discord.application.dto.usecases.CreateUserRequest;
import org.acme.discord.application.dto.usecases.UpdateUserRequest;
import org.acme.discord.infrastructure.mapper.UserMapper;
import org.acme.discord.infrastructure.persistence.GuildEntity;
import org.acme.discord.infrastructure.persistence.RoleEntity;
import org.acme.discord.infrastructure.persistence.UserEntity;
import org.acme.discord.infrastructure.persistence.UserRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * Application service implementing User CRUD use cases.
 */
@ApplicationScoped
public class  UserService {

    private static final String USER_NOT_FOUND_MESSAGE = "User not found";
    private static final String USERNAME_CONFLICT_MESSAGE = "Username is already in use";
    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Creates and persists a user.
     *
     * @param request validated creation request
     * @return created user
     */
    @Transactional
    public UserDTO create(CreateUserRequest request) {
        ensureUsernameAvailable(request.username());
        UserEntity entity = UserMapper.toEntity(request);
        try {
            userRepository.persistAndFlush(entity);
        } catch (PersistenceException exception) {
            throw translateUniqueViolation(exception);
        }
        return UserMapper.toDto(entity);
    }

    /**
     * Retrieves a user by identifier.
     *
     * @param id user identifier
     * @return matching user
     */
    @Transactional
    public UserDTO get(UUID id) {
        return UserMapper.toDto(findEntity(id));
    }

    /**
     * Lists users using stable database ordering and pagination.
     *
     * @param page zero-based page index
     * @param size page size
     * @return paginated users
     */
    @Transactional
    public PageDTO<UserDTO> list(int page, int size) {
        validatePagination(page, size);
        PanacheQuery<UserEntity> query = userRepository.findAll(Sort.ascending("joinedAt", "id"));
        long totalItems = query.count();
        List<UserDTO> items = query.page(Page.of(page, size)).list().stream()
                .map(UserMapper::toDto)
                .toList();
        int totalPages = totalItems == 0 ? 0 : (int) ((totalItems + size - 1) / size);
        return new PageDTO<>(items, page, size, totalItems, totalPages);
    }

    /**
     * Fully updates a user's editable fields.
     *
     * @param id user identifier
     * @param request validated update request
     * @return updated user
     */
    @Transactional
    public UserDTO update(UUID id, UpdateUserRequest request) {
        UserEntity entity = findEntity(id);
        if (userRepository.existsByUsernameAndIdNot(request.username(), id)) {
            throw usernameConflict();
        }
        UserMapper.updateEntity(request, entity);
        try {
            userRepository.flush();
        } catch (PersistenceException exception) {
            throw translateUniqueViolation(exception);
        }
        return UserMapper.toDto(entity);
    }

    /**
     * Deletes a user and its membership and role assignment rows.
     *
     * @param id user identifier
     */
    @Transactional
    public void delete(UUID id) {
        UserEntity entity = findEntity(id);
        Set<GuildEntity> guilds = Set.copyOf(entity.getGuilds());
        Set<RoleEntity> roles = Set.copyOf(entity.getRoles());

        guilds.forEach(guild -> guild.getMembers().remove(entity));
        roles.forEach(role -> role.getUsers().remove(entity));
        entity.getGuilds().clear();
        entity.getRoles().clear();

        userRepository.flush();
        userRepository.delete(entity);
        userRepository.flush();
    }

    private UserEntity findEntity(UUID id) {
        return userRepository.findByIdOptional(id)
                .orElseThrow(() -> new NoSuchElementException(USER_NOT_FOUND_MESSAGE));
    }

    private void ensureUsernameAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
            throw usernameConflict();
        }
    }

    private static void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }

    private static IllegalStateException usernameConflict() {
        return new IllegalStateException(USERNAME_CONFLICT_MESSAGE);
    }

    private static RuntimeException translateUniqueViolation(PersistenceException exception) {
        if (isUniqueViolation(exception)) {
            return usernameConflict();
        }
        return exception;
    }

    private static boolean isUniqueViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException
                    && UNIQUE_VIOLATION_SQL_STATE.equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
