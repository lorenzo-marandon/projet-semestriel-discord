package org.acme.discord.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

/**
 * Panache repository for {@link UserEntity}.
 */
@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<UserEntity, UUID> {

    /**
     * Loads a user while serializing changes to its memberships and role assignments.
     *
     * @param id user identifier
     * @return matching locked user, if present
     */
    public Optional<UserEntity> findByIdForUpdate(UUID id) {
        return findByIdOptional(id, LockModeType.PESSIMISTIC_WRITE);
    }

    /**
     * Builds an unpaged query for a guild's members using stable ordering.
     *
     * @param guildId guild identifier
     * @return unpaged guild member query
     */
    public PanacheQuery<UserEntity> findByGuildId(UUID guildId) {
        return find("select distinct user from UserEntity user join user.guilds guild "
                + "where guild.id = ?1 order by user.joinedAt, user.id", guildId);
    }

    /**
     * Builds a stably ordered user query with an optional literal username filter.
     *
     * @param username optional normalized username fragment
     * @return unpaged query
     */
    public PanacheQuery<UserEntity> findForList(String username) {
        Sort sort = Sort.ascending("joinedAt", "id");
        if (username == null) {
            return findAll(sort);
        }
        return find("lower(username) like ?1 escape '!'", sort, LikePattern.containsLiteral(username));
    }

    /**
     * Checks whether a username is already persisted.
     *
     * @param username username to check
     * @return {@code true} when the username is already used
     */
    public boolean existsByUsername(String username) {
        return count("username", username) > 0;
    }

    /**
     * Checks whether a username is used by another user.
     *
     * @param username username to check
     * @param excludedId user identifier excluded from the check
     * @return {@code true} when another user already uses the username
     */
    public boolean existsByUsernameAndIdNot(String username, UUID excludedId) {
        return count("username = ?1 and id <> ?2", username, excludedId) > 0;
    }
}
