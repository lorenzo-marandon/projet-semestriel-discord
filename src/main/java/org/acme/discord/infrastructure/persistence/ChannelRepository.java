package org.acme.discord.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

/**
 * Panache repository for {@link ChannelEntity}.
 */
@ApplicationScoped
public class ChannelRepository implements PanacheRepositoryBase<ChannelEntity, UUID> {

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
