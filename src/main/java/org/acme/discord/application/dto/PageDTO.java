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
        long totalPages) {

    public PageDTO {
        items = List.copyOf(items);
    }

    /**
     * Creates page metadata without overflow-prone addition.
     *
     * @param items items returned by the database query
     * @param request validated page request
     * @param totalItems number of matching database rows
     * @param <T> response item type
     * @return immutable page response
     */
    public static <T> PageDTO<T> of(List<T> items, PageRequestDTO request, long totalItems) {
        long totalPages = totalItems / request.size();
        if (totalItems % request.size() != 0) {
            totalPages++;
        }
        return new PageDTO<>(items, request.page(), request.size(), totalItems, totalPages);
    }
}
