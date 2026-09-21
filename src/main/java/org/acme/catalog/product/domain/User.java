package org.acme.catalog.product.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Framework-independent domain representation of a user.
 */
public record User(UUID id, String username, String displayName, LocalDateTime joinedAt) {
}
