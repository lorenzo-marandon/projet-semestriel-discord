package org.acme.discord.application.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * Immutable paginated API response.
 *
 * @param items items in the requested page
 * @param page zero-based page index
 * @param size requested page size
 * @param totalItems total number of matching items
 * @param totalPages total number of pages
 * @param <T> response item type
 */
@Schema(description = "Paginated API response")
public record PageDTO<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {

    public PageDTO {
        items = List.copyOf(items);
    }
}
