package org.acme.catalog.product.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Panache repository for {@link UserEntity}.
 */
@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<UserEntity, UUID> {
}
