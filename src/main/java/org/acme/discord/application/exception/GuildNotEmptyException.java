package org.acme.discord.application.exception;

/**
 * Indicates that a guild still owns or references Discord resources.
 */
public class GuildNotEmptyException extends RuntimeException {

    public GuildNotEmptyException() {
        super("Guild must be empty before deletion");
    }
}
