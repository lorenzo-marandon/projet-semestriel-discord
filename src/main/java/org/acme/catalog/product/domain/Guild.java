package org.acme.catalog.product.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Framework-independent domain representation of a guild.
 */
public record Guild(UUID id, String name, String description, LocalDateTime createdAt) {
}
