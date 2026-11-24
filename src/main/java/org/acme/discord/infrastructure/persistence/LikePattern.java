package org.acme.discord.infrastructure.persistence;

import java.util.Locale;

/**
 * Builds escaped LIKE patterns for literal, case-insensitive searches.
 */
final class LikePattern {

    private LikePattern() {
    }

    static String containsLiteral(String value) {
        String escaped = value.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
