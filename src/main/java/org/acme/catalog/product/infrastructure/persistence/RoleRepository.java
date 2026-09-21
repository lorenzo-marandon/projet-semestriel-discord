package org.acme.catalog.product.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Panache repository for {@link RoleEntity}.
 */
@ApplicationScoped
public class RoleRepository implements PanacheRepositoryBase<RoleEntity, UUID> {
}
