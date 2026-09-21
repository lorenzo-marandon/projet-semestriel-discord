package org.acme.catalog.product.domain;

import java.util.UUID;

/**
 * Framework-independent domain representation of a channel.
 */
public record Channel(UUID id, String name, ChannelType type, Integer position) {
}
