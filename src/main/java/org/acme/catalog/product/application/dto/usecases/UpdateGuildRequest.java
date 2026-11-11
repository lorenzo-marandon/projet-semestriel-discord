package org.acme.catalog.product.application.dto.usecases;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Request payload used to fully update a guild's editable fields.
 */
@Schema(description = "Request object to update an existing guild")
public record UpdateGuildRequest(
        @NotBlank(message = "Guild name is required")
        String name,

        String description) {
}
