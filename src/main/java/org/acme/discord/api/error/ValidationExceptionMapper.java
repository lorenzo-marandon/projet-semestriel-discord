package org.acme.discord.api.error;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.discord.application.dto.ValidationViolationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

/**
 * Maps REST endpoint Bean Validation failures to a uniform JSON response.
 */
@Provider
public class ValidationExceptionMapper
        implements ExceptionMapper<ResteasyReactiveViolationException> {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationExceptionMapper.class);

    @Override
    public Response toResponse(ResteasyReactiveViolationException exception) {
        if (exception.getConstraintViolations().stream().anyMatch(ValidationExceptionMapper::isReturnValueViolation)) {
            LOG.error("REST endpoint returned a value that violates its contract", exception);
            return ErrorResponses.internalServerError();
        }

        List<ValidationViolationDTO> violations = exception.getConstraintViolations().stream()
                .map(ValidationExceptionMapper::toViolationDTO)
                .distinct()
                .sorted(Comparator.comparing(ValidationViolationDTO::field)
                        .thenComparing(ValidationViolationDTO::message))
                .toList();
        return ErrorResponses.validation(violations);
    }

    private static ValidationViolationDTO toViolationDTO(ConstraintViolation<?> violation) {
        String parameter = null;
        String property = null;
        for (Path.Node node : violation.getPropertyPath()) {
            if (node.getKind() == ElementKind.PARAMETER) {
                parameter = node.getName();
            } else if (node.getKind() == ElementKind.PROPERTY) {
                property = node.getName();
            } else if (node.getKind() == ElementKind.CROSS_PARAMETER) {
                parameter = "request";
            }
        }
        String field = property != null ? property : parameter != null ? parameter : "request";
        return new ValidationViolationDTO(field, violation.getMessage());
    }

    private static boolean isReturnValueViolation(ConstraintViolation<?> violation) {
        for (Path.Node node : violation.getPropertyPath()) {
            if (node.getKind() == ElementKind.RETURN_VALUE) {
                return true;
            }
        }
        return false;
    }
}
