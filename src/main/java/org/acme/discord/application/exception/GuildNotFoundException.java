package org.acme.discord.application.exception;

/**
 * Indicates that a requested guild does not exist.
 */
public class GuildNotFoundException extends RuntimeException {

    public GuildNotFoundException() {
        super("Guild not found");
    }
}
