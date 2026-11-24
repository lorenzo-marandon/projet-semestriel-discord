package org.acme.discord.application.exception;

/**
 * Indicates that a requested user does not exist.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("User not found");
    }
}
