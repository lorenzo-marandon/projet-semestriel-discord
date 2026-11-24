package org.acme.discord.api.error;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Handles JSON parsing errors.
 * Prevents the client from seeing raw Jackson serialization stack traces
 * if they send a malformed JSON body (e.g., a missing bracket).
 */
@Provider
@Priority(Priorities.USER)
public class JacksonMapper implements ExceptionMapper<MismatchedInputException> {
    @Override
    public jakarta.ws.rs.core.Response toResponse(MismatchedInputException exception) {
        return ErrorResponses.invalidJson();
    }
}
