package org.acme.discord.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks for the generated OpenAPI contract.
 */
@QuarkusTest
class OpenApiContractIT {

    private static final Set<String> BUSINESS_OPERATIONS = Set.of(
            "post /api/users", "get /api/users", "get /api/users/{id}",
            "put /api/users/{id}", "delete /api/users/{id}",
            "post /api/guilds", "get /api/guilds", "get /api/guilds/{id}",
            "put /api/guilds/{id}", "delete /api/guilds/{id}",
            "post /api/roles", "get /api/roles", "get /api/roles/{id}",
            "put /api/roles/{id}", "delete /api/roles/{id}",
            "post /api/channels", "get /api/channels", "get /api/channels/{id}",
            "put /api/channels/{id}", "delete /api/channels/{id}",
            "put /api/guilds/{guildId}/members/{userId}",
            "delete /api/guilds/{guildId}/members/{userId}",
            "get /api/guilds/{guildId}/members",
            "put /api/users/{userId}/roles/{roleId}",
            "delete /api/users/{userId}/roles/{roleId}",
            "get /api/users/{userId}/roles");

    private static final Map<String, String> PAGED_RESPONSES = Map.of(
            "/api/users", "UserDTO",
            "/api/guilds", "GuildDTO",
            "/api/roles", "RoleDTO",
            "/api/channels", "ChannelDTO",
            "/api/guilds/{guildId}/members", "UserDTO",
            "/api/users/{userId}/roles", "RoleDTO");

    @Inject
    ObjectMapper objectMapper;

    @Test
    void shouldServeSwaggerUiAtConfiguredPath() {
        given()
                .when().get("/swagger-ui/")
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .body(containsString("SwaggerUIBundle"));
    }

    @Test
    void shouldDocumentEveryBusinessOperationAndGeneralMetadata() throws IOException {
        JsonNode document = document();

        assertEquals("Discord-like REST API", document.at("/info/title").asText());
        assertEquals("1.0.0-SNAPSHOT", document.at("/info/version").asText());
        assertTrue(document.at("/info/description").asText().contains("guild membership"));
        assertEquals(Set.of("Users", "Guilds", "Roles", "Channels"), tagNames(document));
        assertEquals(BUSINESS_OPERATIONS, businessOperations(document));
        assertFalse(document.at("/paths").has("/api/test/internal-error"));

        BUSINESS_OPERATIONS.forEach(operationKey -> {
            String[] parts = operationKey.split(" ", 2);
            JsonNode operation = operation(document, parts[1], parts[0]);
            assertFalse(operation.path("summary").asText().isBlank(), operationKey + " has no summary");
            assertFalse(operation.path("description").asText().isBlank(), operationKey + " has no description");
            assertTrue(operation.path("tags").isArray(), operationKey + " has no tag");
        });
    }

    @Test
    void shouldDocumentPaginationFiltersAndConcreteItemSchemas() throws IOException {
        JsonNode document = document();

        PAGED_RESPONSES.forEach((path, itemSchema) -> {
            JsonNode operation = operation(document, path, "get");
            assertPaginationParameter(operation, "page", "0", "0", null);
            assertPaginationParameter(operation, "size", "20", "1", "100");

            String pageReference = operation.at("/responses/200/content/application~1json/schema/$ref").asText();
            assertFalse(pageReference.isBlank(), path + " does not reference a concrete page schema");
            JsonNode pageSchema = resolve(document, pageReference);
            assertEquals("#/components/schemas/" + itemSchema,
                    pageSchema.at("/properties/items/items/$ref").asText(), path + " item schema");
            assertPageMetadata(pageSchema);
        });

        assertParameter(operation(document, "/api/users", "get"), "username", "query");
        assertParameter(operation(document, "/api/guilds", "get"), "name", "query");
        assertUuidParameter(operation(document, "/api/roles", "get"), "guildId");
        assertUuidParameter(operation(document, "/api/channels", "get"), "guildId");
        JsonNode type = parameter(operation(document, "/api/channels", "get"), "type");
        assertEquals(Set.of("TEXT", "VOICE"), values(type.at("/schema/enum")));
    }

    @Test
    void shouldDocumentRequestsResponsesAndErrors() throws IOException {
        JsonNode document = document();

        assertRequestSchema(document, "/api/users", "post", "CreateUserRequest");
        assertRequestSchema(document, "/api/guilds/{id}", "put", "UpdateGuildRequest");
        assertRequestSchema(document, "/api/roles", "post", "CreateRoleRequest");
        assertRequestSchema(document, "/api/channels/{id}", "put", "UpdateChannelRequest");

        assertResponseSchema(document, "/api/users", "post", "201", "UserDTO");
        assertResponseSchema(document, "/api/guilds", "post", "201", "GuildDTO");
        assertResponseSchema(document, "/api/roles", "post", "201", "RoleDTO");
        assertResponseSchema(document, "/api/channels", "post", "201", "ChannelDTO");
        assertEquals("string", operation(document, "/api/users", "post")
                .at("/responses/201/headers/Location/schema/type").asText());

        assertResponses(document, "/api/users", "post", "201", "400", "409", "415", "500");
        assertResponses(document, "/api/guilds/{id}", "delete", "204", "400", "404", "409", "500");
        assertResponses(document, "/api/roles/{id}", "delete", "204", "400", "404", "409", "500");
        assertResponses(document, "/api/users/{userId}/roles/{roleId}", "put", "204", "400", "404", "409", "500");

        JsonNode error = document.at("/components/schemas/ErrorDTO");
        assertEquals(Set.of("code", "message", "timestamp", "violations"), fieldNames(error.path("properties")));
        assertEquals("#/components/schemas/ValidationViolationDTO",
                error.at("/properties/violations/items/$ref").asText());
        assertEquals("date-time", resolve(document,
                error.at("/properties/timestamp/$ref").asText()).path("format").asText());
        assertFalse(operation(document, "/api/users/{id}", "delete").at("/responses/204").has("content"));
    }

    @Test
    void shouldExposeOnlyApiDtosAndResolvedReferences() throws IOException {
        JsonNode document = document();
        JsonNode schemas = document.at("/components/schemas");

        for (String schema : Set.of("UserDTO", "GuildDTO", "RoleDTO", "ChannelDTO", "ErrorDTO",
                "ValidationViolationDTO", "CreateUserRequest", "UpdateUserRequest",
                "CreateGuildRequest", "UpdateGuildRequest", "CreateRoleRequest", "UpdateRoleRequest",
                "CreateChannelRequest", "UpdateChannelRequest")) {
            assertTrue(schemas.has(schema), "Missing schema " + schema);
        }
        schemas.fieldNames().forEachRemaining(name -> {
            assertFalse(name.endsWith("Entity"), "Persistence schema exposed: " + name);
            assertFalse(name.contains("Panache"), "Panache schema exposed: " + name);
        });
        assertAllReferencesResolve(document, document);
    }

    private JsonNode document() throws IOException {
        String json = given()
                .accept("application/json")
                .when().get("/q/openapi")
                .then().statusCode(200)
                .extract().asString();
        return objectMapper.readTree(json);
    }

    private static JsonNode operation(JsonNode document, String path, String method) {
        JsonNode operation = document.at("/paths/" + escape(path) + "/" + method);
        assertFalse(operation.isMissingNode(), method.toUpperCase() + " " + path + " is undocumented");
        return operation;
    }

    private static void assertPaginationParameter(
            JsonNode operation, String name, String defaultValue, String minimum, String maximum) {
        JsonNode schema = parameter(operation, name).path("schema");
        assertEquals("integer", schema.path("type").asText(), name + " type");
        assertEquals(defaultValue, schema.path("default").asText(), name + " default");
        assertEquals(minimum, schema.path("minimum").asText(), name + " minimum");
        if (maximum != null) {
            assertEquals(maximum, schema.path("maximum").asText(), name + " maximum");
        }
    }

    private static void assertParameter(JsonNode operation, String name, String location) {
        assertEquals(location, parameter(operation, name).path("in").asText());
    }

    private static void assertUuidParameter(JsonNode operation, String name) {
        JsonNode value = parameter(operation, name);
        assertEquals("query", value.path("in").asText());
        assertEquals("uuid", value.at("/schema/format").asText());
    }

    private static JsonNode parameter(JsonNode operation, String name) {
        for (JsonNode parameter : operation.path("parameters")) {
            if (name.equals(parameter.path("name").asText())) {
                return parameter;
            }
        }
        throw new AssertionError("Missing parameter " + name);
    }

    private static void assertPageMetadata(JsonNode pageSchema) {
        assertEquals(Set.of("items", "page", "size", "totalItems", "totalPages"),
                fieldNames(pageSchema.path("properties")));
    }

    private static void assertRequestSchema(
            JsonNode document, String path, String method, String expectedSchema) {
        JsonNode operation = operation(document, path, method);
        assertTrue(operation.at("/requestBody/required").asBoolean(), method + " " + path + " request required");
        assertEquals("#/components/schemas/" + expectedSchema,
                operation.at("/requestBody/content/application~1json/schema/$ref").asText());
    }

    private static void assertResponseSchema(
            JsonNode document, String path, String method, String response, String expectedSchema) {
        assertEquals("#/components/schemas/" + expectedSchema, operation(document, path, method)
                .at("/responses/" + response + "/content/application~1json/schema/$ref").asText());
    }

    private static void assertResponses(
            JsonNode document, String path, String method, String... expectedResponses) {
        JsonNode responses = operation(document, path, method).path("responses");
        for (String response : expectedResponses) {
            assertTrue(responses.has(response), method.toUpperCase() + " " + path + " missing " + response);
        }
    }

    private static void assertAllReferencesResolve(JsonNode document, JsonNode node) {
        if (node.isObject() && node.has("$ref")) {
            resolve(document, node.path("$ref").asText());
        }
        if (node.isObject()) {
            node.properties().forEach(entry -> assertAllReferencesResolve(document, entry.getValue()));
        } else if (node.isArray()) {
            node.forEach(child -> assertAllReferencesResolve(document, child));
        }
    }

    private static JsonNode resolve(JsonNode document, String reference) {
        assertTrue(reference.startsWith("#/"), "External or invalid reference: " + reference);
        JsonNode resolved = document.at(reference.substring(1));
        assertFalse(resolved.isMissingNode(), "Unresolved reference: " + reference);
        return resolved;
    }

    private static Set<String> businessOperations(JsonNode document) {
        Set<String> methods = Set.of("get", "post", "put", "delete", "patch");
        java.util.HashSet<String> result = new java.util.HashSet<>();
        document.path("paths").properties().forEach(path -> path.getValue().fieldNames().forEachRemaining(method -> {
            if (path.getKey().startsWith("/api/") && methods.contains(method)) {
                result.add(method + " " + path.getKey());
            }
        }));
        return result;
    }

    private static Set<String> tagNames(JsonNode document) {
        java.util.HashSet<String> result = new java.util.HashSet<>();
        document.path("tags").forEach(tag -> result.add(tag.path("name").asText()));
        return result;
    }

    private static Set<String> fieldNames(JsonNode node) {
        java.util.HashSet<String> result = new java.util.HashSet<>();
        node.fieldNames().forEachRemaining(result::add);
        return result;
    }

    private static Set<String> values(JsonNode node) {
        java.util.HashSet<String> result = new java.util.HashSet<>();
        node.forEach(value -> result.add(value.asText()));
        return result;
    }

    private static String escape(String path) {
        return path.replace("~", "~0").replace("/", "~1");
    }
}
