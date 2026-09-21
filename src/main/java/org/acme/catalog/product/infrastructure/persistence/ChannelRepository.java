package org.acme.catalog.product.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Panache repository for {@link ChannelEntity}.
 */
@ApplicationScoped
public class ChannelRepository implements PanacheRepositoryBase<ChannelEntity, UUID> {
}
