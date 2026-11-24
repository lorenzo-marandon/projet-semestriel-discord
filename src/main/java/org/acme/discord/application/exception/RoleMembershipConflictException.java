package org.acme.discord.application.exception;

/**
 * Indicates that a role cannot be assigned because the user is not a member
 * of the role's guild.
 */
public class RoleMembershipConflictException extends RuntimeException {

    public RoleMembershipConflictException() {
        super("User must belong to the role's guild before receiving the role");
    }
}
