package org.acme.discord.application.exception;

/**
 * Indicates that a role is still assigned to one or more users.
 */
public class RoleInUseException extends RuntimeException {

    public RoleInUseException() {
        super("Role is assigned to one or more users");
    }
}
