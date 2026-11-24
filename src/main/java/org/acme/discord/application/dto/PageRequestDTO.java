package org.acme.discord.application.dto;

import org.acme.discord.application.exception.InvalidRequestException;

/**
 * Validated zero-based pagination request.
 *
 * @param page zero-based page index
 * @param size number of items requested per page
 */
public record PageRequestDTO(int page, int size) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageRequestDTO {
        if (page < 0) {
            throw new InvalidRequestException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidRequestException("Size must be between 1 and " + MAX_SIZE);
        }
        if ((long) page * size > Integer.MAX_VALUE) {
            throw new InvalidRequestException("Pagination offset is too large");
        }
    }
}
