package org.acme.discord.application.exception;

/**
 * Indicates that a requested channel does not exist.
 */
public class ChannelNotFoundException extends RuntimeException {

    public ChannelNotFoundException() {
        super("Channel not found");
    }
}
