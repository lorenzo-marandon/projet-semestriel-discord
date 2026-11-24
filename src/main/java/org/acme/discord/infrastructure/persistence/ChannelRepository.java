package org.acme.discord.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import org.acme.discord.domain.ChannelType;

import java.util.Optional;
import java.util.UUID;

/**
 * Panache repository for {@link ChannelEntity}.
 */
@ApplicationScoped
public class ChannelRepository implements PanacheRepositoryBase<ChannelEntity, UUID> {

    /**
     * Builds a stably ordered channel query from optional exact filters.
     *
     * @param guildId optional guild identifier
     * @param type optional channel type
     * @return unpaged query
     */
    public PanacheQuery<ChannelEntity> findForList(UUID guildId, ChannelType type) {
        Sort sort = Sort.ascending("position", "id");
        if (guildId != null && type != null) {
            return find("guild.id = ?1 and type = ?2", sort, guildId, type);
        }
        if (guildId != null) {
            return find("guild.id", sort, guildId);
        }
        if (type != null) {
            return find("type", sort, type);
        }
        return findAll(sort);
    }

    /**
     * Loads a channel while preventing concurrent updates or deletion.
     *
     * @param id channel identifier
     * @return matching locked channel, if present
     */
    public Optional<ChannelEntity> findByIdForUpdate(UUID id) {
        return findByIdOptional(id, LockModeType.PESSIMISTIC_WRITE);
    }
}
