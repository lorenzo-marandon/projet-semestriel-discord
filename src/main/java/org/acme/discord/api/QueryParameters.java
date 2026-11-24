package org.acme.discord.api;

import org.acme.discord.application.dto.PageRequestDTO;
import org.acme.discord.application.exception.InvalidRequestException;

import java.util.UUID;

/**
 * Shared parsing and validation for REST query parameters.
 */
final class QueryParameters {

    static final String DEFAULT_PAGE = "0";
    static final String DEFAULT_SIZE = "20";

    private QueryParameters() {
    }

    static PageRequestDTO pagination(String page, String size) {
        return new PageRequestDTO(parseInteger(page, "page"), parseInteger(size, "size"));
    }

    static String optionalText(String value, String parameterName) {
        return optionalText(value, parameterName, value != null);
    }

    static String optionalText(String value, String parameterName, boolean supplied) {
        if (!supplied) {
            return null;
        }
        if (value == null) {
            throw new InvalidRequestException(parameterName + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new InvalidRequestException(parameterName + " must not be blank");
        }
        return normalized;
    }

    static UUID optionalUuid(String value, String parameterName) {
        return optionalUuid(value, parameterName, value != null);
    }

    static UUID optionalUuid(String value, String parameterName, boolean supplied) {
        String normalized = optionalText(value, parameterName, supplied);
        if (normalized == null) {
            return null;
        }
        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Invalid " + parameterName);
        }
    }

    private static int parseInteger(String value, String parameterName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new InvalidRequestException(parameterName + " must be an integer");
        }
    }
}
