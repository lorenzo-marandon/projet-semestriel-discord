package org.acme.discord.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Panache repository for {@link RoleEntity}.
 */
@ApplicationScoped
public class RoleRepository implements PanacheRepositoryBase<RoleEntity, UUID> {

    private static final String FOREIGN_KEY_VIOLATION_SQL_STATE = "23503";
    private static final String USER_ROLES_TABLE = "user_roles";

    /**
     * Loads a role while preventing concurrent updates or deletion.
     *
     * @param id role identifier
     * @return matching locked role, if present
     */
    public Optional<RoleEntity> findByIdForUpdate(UUID id) {
        return findByIdOptional(id, LockModeType.PESSIMISTIC_WRITE);
    }

    /**
     * Builds an unpaged query for a user's assigned roles using stable ordering.
     *
     * @param userId user identifier
     * @return unpaged assigned role query
     */
    public PanacheQuery<RoleEntity> findByUserId(UUID userId) {
        return find("select distinct role from RoleEntity role join role.users user "
                + "where user.id = ?1 order by role.position, role.id", userId);
    }

    /**
     * Builds a stably ordered role query with an optional guild filter.
     *
     * @param guildId optional guild identifier
     * @return unpaged query
     */
    public PanacheQuery<RoleEntity> findForList(UUID guildId) {
        Sort sort = Sort.ascending("position", "id");
        return guildId == null ? findAll(sort) : find("guild.id", sort, guildId);
    }

    /**
     * Checks whether a role is assigned to at least one user without loading the collection.
     *
     * @param roleId role identifier
     * @return {@code true} when a user references the role
     */
    public boolean hasUsers(UUID roleId) {
        return !getEntityManager().createQuery(
                        "select 1 from RoleEntity role join role.users user where role.id = :roleId",
                        Integer.class)
                .setParameter("roleId", roleId)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }

    /**
     * Checks whether a persistence failure is caused by a user-role reference.
     *
     * @param exception persistence failure
     * @return {@code true} only for a foreign key violation reported on {@code user_roles}
     */
    public boolean isUserRoleForeignKeyViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException
                    && FOREIGN_KEY_VIOLATION_SQL_STATE.equals(sqlException.getSQLState())) {
                String diagnostic = sqlException.getMessage();
                return diagnostic != null && diagnostic.contains(USER_ROLES_TABLE);
            }
            current = current.getCause();
        }
        return false;
    }
}
