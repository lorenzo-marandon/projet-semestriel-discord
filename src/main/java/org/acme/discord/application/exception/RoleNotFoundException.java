package org.acme.discord.application.exception;

/**
 * Indicates that a requested role does not exist.
 */
public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException() {
        super("Role not found");
    }
}
