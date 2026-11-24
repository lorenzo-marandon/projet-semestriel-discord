package org.acme.discord.api;

import org.acme.discord.application.dto.PageDTO;
import org.acme.discord.application.dto.PageRequestDTO;
import org.acme.discord.application.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryParametersTest {

    @Test
    void shouldParseValidPaginationAndTrimFilters() {
        PageRequestDTO pagination = QueryParameters.pagination("2", "50");

        assertEquals(2, pagination.page());
        assertEquals(50, pagination.size());
        assertEquals("search", QueryParameters.optionalText("  search  ", "name"));
        assertNull(QueryParameters.optionalText(null, "name"));
    }

    @Test
    void shouldRejectInvalidAndOverflowingPagination() {
        assertThrows(InvalidRequestException.class,
                () -> QueryParameters.pagination("-1", "20"));
        assertThrows(InvalidRequestException.class,
                () -> QueryParameters.pagination("0", "0"));
        assertThrows(InvalidRequestException.class,
                () -> QueryParameters.pagination("0", "101"));
        assertThrows(InvalidRequestException.class,
                () -> QueryParameters.pagination("not-a-number", "20"));
        assertThrows(InvalidRequestException.class,
                () -> QueryParameters.pagination("999999999999999999999", "20"));
        assertThrows(InvalidRequestException.class,
                () -> QueryParameters.pagination(Integer.toString(Integer.MAX_VALUE), "100"));
        assertEquals(Long.MAX_VALUE,
                PageDTO.of(List.of(), new PageRequestDTO(0, 1), Long.MAX_VALUE).totalPages());
    }

    @Test
    void shouldParseOptionalUuidAndRejectInvalidFilters() {
        UUID id = UUID.randomUUID();

        assertEquals(id, QueryParameters.optionalUuid("  " + id + "  ", "guildId"));
        assertNull(QueryParameters.optionalUuid(null, "guildId"));
        assertThrows(InvalidRequestException.class,
                () -> QueryParameters.optionalUuid("not-a-uuid", "guildId"));
        assertThrows(InvalidRequestException.class,
                () -> QueryParameters.optionalText("   ", "username"));
        assertThrows(InvalidRequestException.class,
                () -> QueryParameters.optionalText(null, "username", true));
    }
}
