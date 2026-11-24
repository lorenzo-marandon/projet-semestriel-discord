package org.acme.discord.api.error;

import com.fasterxml.jackson.core.JsonParseException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps syntactically malformed JSON to the common client-safe error format.
 */
@Provider
@Priority(Priorities.USER)
public class JsonParseMapper implements ExceptionMapper<JsonParseException> {

    @Override
    public Response toResponse(JsonParseException exception) {
        return ErrorResponses.invalidJson();
    }
}
