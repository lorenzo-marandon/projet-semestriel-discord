package org.acme.catalog.product.application.dto.usecases;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.acme.catalog.product.domain.ChannelType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void shouldAcceptValidCreateAndUpdateRequests() {
        UUID guildId = UUID.randomUUID();
        assertValid(new CreateUserRequest("alice", "Alice"));
        assertValid(new UpdateUserRequest("alice-updated", "Alice Updated"));
        assertValid(new CreateGuildRequest("Guild", null));
        assertValid(new UpdateGuildRequest("Guild Updated", "Description"));
        assertValid(new CreateRoleRequest("Member", "#00AA00", 0, guildId));
        assertValid(new UpdateRoleRequest("Moderator", "#3366FF", 1));
        assertValid(new CreateChannelRequest("general", ChannelType.TEXT, 0, guildId));
        assertValid(new UpdateChannelRequest("voice", ChannelType.VOICE, 1));
    }

    @Test
    void shouldRejectBlankRequiredTextFields() {
        UUID guildId = UUID.randomUUID();
        assertInvalidProperties(new CreateUserRequest(" ", ""), "username", "displayName");
        assertInvalidProperties(new UpdateUserRequest("", " "), "username", "displayName");
        assertInvalidProperties(new CreateGuildRequest(" ", null), "name");
        assertInvalidProperties(new UpdateGuildRequest("", "Description"), "name");
        assertInvalidProperties(new CreateRoleRequest("", " ", 0, guildId), "name", "color");
        assertInvalidProperties(new UpdateRoleRequest(" ", "", 0), "name", "color");
        assertInvalidProperties(
                new CreateChannelRequest("", ChannelType.TEXT, 0, guildId), "name");
        assertInvalidProperties(new UpdateChannelRequest(" ", ChannelType.VOICE, 0), "name");
    }

    @Test
    void shouldRejectMissingOrNegativeTypedFields() {
        assertInvalidProperties(new CreateRoleRequest("Role", "#000000", null, null),
                "position", "guildId");
        assertInvalidProperties(new UpdateRoleRequest("Role", "#000000", -1), "position");
        assertInvalidProperties(new CreateChannelRequest("general", null, null, null),
                "type", "position", "guildId");
        assertInvalidProperties(new UpdateChannelRequest("general", null, -1),
                "type", "position");
    }

    @Test
    void shouldNotExposeServerManagedFieldsInRequests() {
        assertComponents(CreateUserRequest.class, "username", "displayName");
        assertComponents(UpdateUserRequest.class, "username", "displayName");
        assertComponents(CreateGuildRequest.class, "name", "description");
        assertComponents(UpdateGuildRequest.class, "name", "description");
        assertComponents(CreateRoleRequest.class, "name", "color", "position", "guildId");
        assertComponents(UpdateRoleRequest.class, "name", "color", "position");
        assertComponents(CreateChannelRequest.class, "name", "type", "position", "guildId");
        assertComponents(UpdateChannelRequest.class, "name", "type", "position");
    }

    private static void assertValid(Object request) {
        assertTrue(validator.validate(request).isEmpty());
    }

    private static void assertInvalidProperties(Object request, String... expectedProperties) {
        Set<String> properties = validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
        assertEquals(Set.of(expectedProperties), properties);
    }

    private static void assertComponents(Class<? extends Record> type, String... expectedComponents) {
        Set<String> actualComponents = Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of(expectedComponents), actualComponents);
        assertFalse(actualComponents.contains("id"));
        assertFalse(actualComponents.contains("createdAt"));
        assertFalse(actualComponents.contains("joinedAt"));
    }
}
