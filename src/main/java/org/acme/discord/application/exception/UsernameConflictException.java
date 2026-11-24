package org.acme.discord.application.exception;

/**
 * Indicates that a username is already assigned to another user.
 */
public class UsernameConflictException extends RuntimeException {

    public UsernameConflictException() {
        super("Username is already in use");
    }
}
