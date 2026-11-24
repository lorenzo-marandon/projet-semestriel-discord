package org.acme.discord.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

/**
 * Panache repository for {@link GuildEntity}.
 */
@ApplicationScoped
public class GuildRepository implements PanacheRepositoryBase<GuildEntity, UUID> {

    /**
     * Builds a stably ordered guild query with an optional literal name filter.
     *
     * @param name optional normalized guild name fragment
     * @return unpaged query
     */
    public PanacheQuery<GuildEntity> findForList(String name) {
        Sort sort = Sort.ascending("createdAt", "id");
        if (name == null) {
            return findAll(sort);
        }
        return find("lower(name) like ?1 escape '!'", sort, LikePattern.containsLiteral(name));
    }

    /**
     * Loads a guild while preventing concurrent updates or deletion.
     *
     * @param id guild identifier
     * @return matching locked guild, if present
     */
    public Optional<GuildEntity> findByIdForUpdate(UUID id) {
        return findByIdOptional(id, LockModeType.PESSIMISTIC_WRITE);
    }

    /**
     * Loads a guild with a shared lock for creating dependent resources.
     *
     * @param id guild identifier
     * @return matching locked guild, if present
     */
    public Optional<GuildEntity> findByIdForRead(UUID id) {
        return findByIdOptional(id, LockModeType.PESSIMISTIC_READ);
    }

    /**
     * Checks whether a guild has at least one member without loading the collection.
     *
     * @param guildId guild identifier
     * @return {@code true} when a member references the guild
     */
    public boolean hasMembers(UUID guildId) {
        return exists("select 1 from GuildEntity guild join guild.members member "
                + "where guild.id = :guildId", guildId);
    }

    /**
     * Checks whether a guild has at least one role without loading the collection.
     *
     * @param guildId guild identifier
     * @return {@code true} when a role references the guild
     */
    public boolean hasRoles(UUID guildId) {
        return exists("select 1 from GuildEntity guild join guild.roles role "
                + "where guild.id = :guildId", guildId);
    }

    /**
     * Checks whether a guild has at least one channel without loading the collection.
     *
     * @param guildId guild identifier
     * @return {@code true} when a channel references the guild
     */
    public boolean hasChannels(UUID guildId) {
        return exists("select 1 from GuildEntity guild join guild.channels channel "
                + "where guild.id = :guildId", guildId);
    }

    private boolean exists(String query, UUID guildId) {
        return !getEntityManager().createQuery(query, Integer.class)
                .setParameter("guildId", guildId)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }
}
