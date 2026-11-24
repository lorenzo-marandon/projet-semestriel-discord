package org.acme.discord.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Client-safe description of one request validation failure.
 *
 * @param field invalid request field or parameter
 * @param message validation message
 */
@Schema(description = "Request validation violation")
public record ValidationViolationDTO(String field, String message) {
}
