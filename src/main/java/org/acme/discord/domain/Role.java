package org.acme.discord.domain;

import java.util.UUID;

/**
 * Framework-independent domain representation of a role.
 */
public record Role(UUID id, String name, String color, Integer position) {
}
