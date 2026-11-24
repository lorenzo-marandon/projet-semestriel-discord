package org.acme.discord.api;

/**
 * Client-safe examples shared by OpenAPI response annotations.
 */
final class OpenApiExamples {

    static final String VALIDATION_ERROR = """
            {"code":"VALIDATION_ERROR","message":"Request validation failed",\
            "timestamp":"2026-01-15T10:45:00",\
            "violations":[{"field":"username","message":"Username is required"}]}""";

    static final String INVALID_REQUEST = """
            {"code":"INVALID_REQUEST","message":"Invalid user id",\
            "timestamp":"2026-01-15T10:45:00","violations":[]}""";

    static final String NOT_FOUND = """
            {"code":"USER_NOT_FOUND","message":"User not found",\
            "timestamp":"2026-01-15T10:45:00","violations":[]}""";

    static final String CONFLICT = """
            {"code":"USERNAME_ALREADY_EXISTS","message":"Username is already in use",\
            "timestamp":"2026-01-15T10:45:00","violations":[]}""";

    static final String INTERNAL_ERROR = """
            {"code":"INTERNAL_SERVER_ERROR",\
            "message":"An unexpected error occurred. Please contact support.",\
            "timestamp":"2026-01-15T10:45:00","violations":[]}""";

    private OpenApiExamples() {
    }
}
