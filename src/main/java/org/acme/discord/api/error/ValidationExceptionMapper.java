package org.acme.discord.api.error;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.validation.ConstraintViolation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.dto.ErrorDTO;

import java.util.stream.Collectors;

/**
 * Maps REST endpoint Bean Validation failures to a uniform JSON response.
 */
@Provider
public class ValidationExceptionMapper
        implements ExceptionMapper<ResteasyReactiveViolationException> {

    @Override
    public Response toResponse(ResteasyReactiveViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorDTO.of("VALIDATION_ERROR", message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
