package org.acme.discord.api;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.acme.testresources.PostgreSqlTestResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end REST workflow spanning the four resources and both association types.
 */
@QuarkusTest
@QuarkusTestResource(PostgreSqlTestResource.class)
class DiscordWorkflowIT {

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        QuarkusTransaction.requiringNew().run(() -> {
            entityManager.createNativeQuery("DELETE FROM user_roles").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM guild_members").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM channels").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM roles").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM guilds").executeUpdate();
        });
    }

    @Test
    void shouldCompleteDiscordLifecycleAndPreserveRelationalIntegrity() {
        UUID userId = create("/api/users", Map.of(
                "username", "workflow-user", "displayName", "Workflow User"));
        UUID guildId = create("/api/guilds", Map.of(
                "name", "Workflow Guild", "description", "Integration workflow"));
        UUID roleId = create("/api/roles", Map.of(
                "name", "Workflow Role", "color", "blue", "position", 1,
                "guildId", guildId));
        UUID channelId = create("/api/channels", Map.of(
                "name", "workflow-channel", "type", "TEXT", "position", 1,
                "guildId", guildId));

        given().contentType("application/json")
                .put("/api/guilds/{guildId}/members/{userId}", guildId, userId)
                .then().statusCode(204);
        given().contentType("application/json")
                .put("/api/users/{userId}/roles/{roleId}", userId, roleId)
                .then().statusCode(204);

        given().get("/api/guilds/{guildId}/members", guildId)
                .then().statusCode(200)
                .body("items", hasSize(1))
                .body("items[0].id", equalTo(userId.toString()))
                .body("totalItems", equalTo(1));
        given().get("/api/users/{userId}/roles", userId)
                .then().statusCode(200)
                .body("items", hasSize(1))
                .body("items[0].id", equalTo(roleId.toString()))
                .body("totalItems", equalTo(1));
        assertEquals(1, count("guild_members"));
        assertEquals(1, count("user_roles"));

        given().delete("/api/guilds/{guildId}/members/{userId}", guildId, userId)
                .then().statusCode(204);

        given().get("/api/guilds/{guildId}/members", guildId)
                .then().statusCode(200).body("items", hasSize(0));
        given().get("/api/users/{userId}/roles", userId)
                .then().statusCode(200).body("items", hasSize(0));
        assertEquals(0, count("guild_members"));
        assertEquals(0, count("user_roles"));

        given().delete("/api/roles/{id}", roleId).then().statusCode(204);
        given().delete("/api/channels/{id}", channelId).then().statusCode(204);
        given().delete("/api/users/{id}", userId).then().statusCode(204);
        given().delete("/api/guilds/{id}", guildId).then().statusCode(204);

        assertNotFound("/api/roles/{id}", roleId, "ROLE_NOT_FOUND");
        assertNotFound("/api/channels/{id}", channelId, "CHANNEL_NOT_FOUND");
        assertNotFound("/api/users/{id}", userId, "USER_NOT_FOUND");
        assertNotFound("/api/guilds/{id}", guildId, "GUILD_NOT_FOUND");
        assertEquals(0, count("roles"));
        assertEquals(0, count("channels"));
        assertEquals(0, count("users"));
        assertEquals(0, count("guilds"));
    }

    private static UUID create(String path, Map<String, Object> request) {
        Response response = given().contentType("application/json").body(request).post(path);
        response.then().statusCode(201);
        UUID id = UUID.fromString(response.path("id"));
        assertTrue(response.header("Location").endsWith(path + "/" + id));
        return id;
    }

    private static void assertNotFound(String path, UUID id, String code) {
        given().get(path, id)
                .then().statusCode(404)
                .body("code", equalTo(code))
                .body("message", notNullValue())
                .body("timestamp", notNullValue())
                .body("violations", hasSize(0));
    }

    private long count(String table) {
        AtomicLong result = new AtomicLong();
        QuarkusTransaction.requiringNew().run(() -> result.set(((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM " + table)
                .getSingleResult()).longValue()));
        return result.get();
    }
}
