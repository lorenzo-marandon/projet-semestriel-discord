package org.acme.discord.api.error;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.discord.application.dto.ErrorDTO;
import org.acme.discord.application.dto.ValidationViolationDTO;
import org.acme.discord.application.dto.usecases.CreateUserRequest;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationExceptionMapperTest {

    @Test
    void shouldReturnSortedFieldViolationsForRestInput() {
        Set<ConstraintViolation<CreateUserRequest>> violations;
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            violations = validator.validate(new CreateUserRequest("", " "));
        }

        Response response = new ValidationExceptionMapper().toResponse(
                new ResteasyReactiveViolationException(violations));
        ErrorDTO error = (ErrorDTO) response.getEntity();

        assertEquals(400, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertEquals("VALIDATION_ERROR", error.code());
        assertEquals("Request validation failed", error.message());
        assertEquals(
                Set.of(
                        new ValidationViolationDTO("displayName", "Display name is required"),
                        new ValidationViolationDTO("username", "Username is required")),
                Set.copyOf(error.violations()));
        assertEquals("displayName", error.violations().getFirst().field());
    }

    @Test
    void shouldTreatNonRestConstraintViolationAsInternalError() {
        ConstraintViolationException exception = new ConstraintViolationException("internal-secret", Set.of());

        Response response = new GlobalExceptionMapper().toResponse(exception);
        ErrorDTO error = (ErrorDTO) response.getEntity();

        assertEquals(500, response.getStatus());
        assertEquals("INTERNAL_SERVER_ERROR", error.code());
        assertFalse(error.message().contains("internal-secret"));
        assertTrue(error.violations().isEmpty());
    }
}
