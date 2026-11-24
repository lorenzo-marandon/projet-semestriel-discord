package org.acme.discord.application.exception;

/**
 * Indicates that a client-supplied REST parameter is invalid.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
