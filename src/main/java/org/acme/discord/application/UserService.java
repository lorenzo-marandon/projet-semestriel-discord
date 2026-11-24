package org.acme.discord.application;

import io.quarkus.panache.common.Page;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.acme.discord.application.dto.PageDTO;
import org.acme.discord.application.dto.PageRequestDTO;
import org.acme.discord.application.dto.UserDTO;
import org.acme.discord.application.dto.usecases.CreateUserRequest;
import org.acme.discord.application.dto.usecases.UpdateUserRequest;
import org.acme.discord.application.exception.UserNotFoundException;
import org.acme.discord.application.exception.UsernameConflictException;
import org.acme.discord.infrastructure.mapper.UserMapper;
import org.acme.discord.infrastructure.persistence.GuildEntity;
import org.acme.discord.infrastructure.persistence.RoleEntity;
import org.acme.discord.infrastructure.persistence.UserEntity;
import org.acme.discord.infrastructure.persistence.UserRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Application service implementing User CRUD use cases.
 */
@ApplicationScoped
public class  UserService {

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
     * @param pagination validated pagination parameters
     * @param username optional normalized username fragment
     * @return paginated users
     */
    @Transactional
    public PageDTO<UserDTO> list(PageRequestDTO pagination, String username) {
        PanacheQuery<UserEntity> query = userRepository.findForList(username);
        long totalItems = query.count();
        List<UserDTO> items = query.page(Page.of(pagination.page(), pagination.size())).list().stream()
                .map(UserMapper::toDto)
                .toList();
        return PageDTO.of(items, pagination, totalItems);
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
        UserEntity entity = userRepository.findByIdForUpdate(id)
                .orElseThrow(UserNotFoundException::new);
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
                .orElseThrow(UserNotFoundException::new);
    }

    private void ensureUsernameAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
            throw usernameConflict();
        }
    }

    private static UsernameConflictException usernameConflict() {
        return new UsernameConflictException();
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
