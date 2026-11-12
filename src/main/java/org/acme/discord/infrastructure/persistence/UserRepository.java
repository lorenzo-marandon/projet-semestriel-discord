package org.acme.discord.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Panache repository for {@link UserEntity}.
 */
@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<UserEntity, UUID> {

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
