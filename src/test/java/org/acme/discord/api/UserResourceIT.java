package org.acme.discord.api;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.acme.discord.infrastructure.persistence.GuildEntity;
import org.acme.discord.infrastructure.persistence.GuildRepository;
import org.acme.discord.infrastructure.persistence.RoleEntity;
import org.acme.discord.infrastructure.persistence.RoleRepository;
import org.acme.discord.infrastructure.persistence.UserEntity;
import org.acme.discord.infrastructure.persistence.UserRepository;
import org.acme.testresources.PostgreSqlTestResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(PostgreSqlTestResource.class)
class UserResourceIT {

    @Inject
    EntityManager entityManager;

    @Inject
    UserRepository userRepository;

    @Inject
    GuildRepository guildRepository;

    @Inject
    RoleRepository roleRepository;

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
    void shouldCreateUserWithLocationAndServerManagedFields() {
        Instant before = Instant.now();

        Response response = given()
                .contentType("application/json")
                .body(Map.of("username", "alice", "displayName", "Alice"))
                .when().post("/api/users")
                .then().statusCode(201)
                .contentType("application/json")
                .body("username", equalTo("alice"))
                .body("displayName", equalTo("Alice"))
                .body("guildIds", hasSize(0))
                .body("roleIds", hasSize(0))
                .extract().response();

        Instant after = Instant.now();
        UUID id = UUID.fromString(response.path("id"));
        LocalDateTime joinedAt = LocalDateTime.parse(response.path("joinedAt"));
        Instant generated = joinedAt.toInstant(ZoneOffset.UTC);

        assertTrue(response.header("Location").endsWith("/api/users/" + id));
        assertFalse(generated.isBefore(before.minusSeconds(1)));
        assertFalse(generated.isAfter(after.plusSeconds(1)));
        assertNotNull(findUser(id));
    }

    @Test
    void shouldRejectInvalidAndDuplicateCreationWithJsonErrors() {
        assertError(given()
                .contentType("application/json")
                .body(Map.of("username", " ", "displayName", ""))
                .post("/api/users"), 400, "VALIDATION_ERROR");

        createUser("duplicate", "First");

        Response duplicate = given()
                .contentType("application/json")
                .body(Map.of("username", "duplicate", "displayName", "Second"))
                .post("/api/users");
        assertError(duplicate, 409, "USERNAME_ALREADY_EXISTS");
        assertFalse(duplicate.asString().toLowerCase().contains("sql"));
    }

    @Test
    void shouldGetExistingUserAndRejectMissingOrMalformedIds() {
        UUID id = persistUser("lookup", "Lookup", LocalDateTime.of(2026, 1, 1, 10, 0));

        given().get("/api/users/{id}", id)
                .then().statusCode(200)
                .contentType("application/json")
                .body("id", equalTo(id.toString()))
                .body("username", equalTo("lookup"));

        assertError(given().get("/api/users/{id}", UUID.randomUUID()),
                404, "USER_NOT_FOUND");
        assertError(given().get("/api/users/not-a-uuid"),
                400, "INVALID_REQUEST");
    }

    @Test
    void shouldReturnEmptyDefaultPage() {
        given().get("/api/users")
                .then().statusCode(200)
                .contentType("application/json")
                .body("items", hasSize(0))
                .body("page", equalTo(0))
                .body("size", equalTo(20))
                .body("totalItems", equalTo(0))
                .body("totalPages", equalTo(0));
    }

    @Test
    void shouldPaginateUsingStableDatabaseOrdering() {
        LocalDateTime firstDate = LocalDateTime.of(2026, 1, 1, 8, 0);
        LocalDateTime secondDate = LocalDateTime.of(2026, 1, 2, 8, 0);
        LocalDateTime thirdDate = LocalDateTime.of(2026, 1, 3, 8, 0);
        UUID firstTie = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID secondTie = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID later = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID last = UUID.fromString("00000000-0000-0000-0000-000000000004");
        insertUser(firstTie, "first-tie", firstDate);
        insertUser(secondTie, "second-tie", firstDate);
        insertUser(later, "later", secondDate);
        insertUser(last, "last", thirdDate);

        given().queryParam("page", 0).queryParam("size", 2).get("/api/users")
                .then().statusCode(200)
                .body("items", hasSize(2))
                .body("items[0].id", equalTo(firstTie.toString()))
                .body("items[1].id", equalTo(secondTie.toString()))
                .body("totalItems", equalTo(4))
                .body("totalPages", equalTo(2));

        given().queryParam("page", 1).queryParam("size", 2).get("/api/users")
                .then().statusCode(200)
                .body("items[0].id", equalTo(later.toString()))
                .body("items[1].id", equalTo(last.toString()));

        given().queryParam("page", 2).queryParam("size", 2).get("/api/users")
                .then().statusCode(200)
                .body("items", hasSize(0))
                .body("totalItems", equalTo(4))
                .body("totalPages", equalTo(2));
    }

    @Test
    void shouldRejectInvalidPagination() {
        assertError(given().queryParam("page", -1).get("/api/users"),
                400, "INVALID_REQUEST");
        assertError(given().queryParam("size", 0).get("/api/users"),
                400, "INVALID_REQUEST");
        assertError(given().queryParam("size", 101).get("/api/users"),
                400, "INVALID_REQUEST");
        assertError(given().queryParam("page", "abc").get("/api/users"),
                400, "INVALID_REQUEST");
    }

    @Test
    void shouldUpdateEditableFieldsAndPreserveServerManagedDataAndRelations() {
        RelatedUser fixture = persistUserWithRelations();

        Response response = given().contentType("application/json")
                .body(Map.of("username", "updated", "displayName", "Updated User"))
                .put("/api/users/{id}", fixture.userId())
                .then().statusCode(200)
                .body("id", equalTo(fixture.userId().toString()))
                .body("username", equalTo("updated"))
                .body("displayName", equalTo("Updated User"))
                .body("guildIds", equalTo(Set.of(fixture.guildId()).stream()
                        .map(UUID::toString).toList()))
                .body("roleIds", equalTo(Set.of(fixture.roleId()).stream()
                        .map(UUID::toString).toList()))
                .extract().response();

        assertEquals(fixture.joinedAt(), LocalDateTime.parse(response.path("joinedAt")));
        UserEntity updated = findUser(fixture.userId());
        assertEquals(fixture.joinedAt(), updated.getJoinedAt());
        assertEquals(1, relationCount("guild_members", fixture.userId()));
        assertEquals(1, relationCount("user_roles", fixture.userId()));
    }

    @Test
    void shouldRejectMissingInvalidOrConflictingUpdate() {
        UUID first = persistUser("first", "First", LocalDateTime.of(2026, 2, 1, 10, 0));
        persistUser("second", "Second", LocalDateTime.of(2026, 2, 2, 10, 0));

        assertError(given().contentType("application/json")
                .body(Map.of("username", "second", "displayName", "Conflict"))
                .put("/api/users/{id}", first), 409, "USERNAME_ALREADY_EXISTS");
        assertError(given().contentType("application/json")
                .body(Map.of("username", "missing", "displayName", "Missing"))
                .put("/api/users/{id}", UUID.randomUUID()), 404, "USER_NOT_FOUND");
        assertError(given().contentType("application/json")
                .body(Map.of("username", "", "displayName", ""))
                .put("/api/users/{id}", first), 400, "VALIDATION_ERROR");
    }

    @Test
    void shouldDeleteSimpleUserAndRejectMissingUser() {
        UUID id = persistUser("delete", "Delete", LocalDateTime.of(2026, 3, 1, 10, 0));

        Response response = given().delete("/api/users/{id}", id);
        response.then().statusCode(204);
        assertEquals("", response.asString());

        assertNull(findUser(id));
        assertError(given().delete("/api/users/{id}", id), 404, "USER_NOT_FOUND");
    }

    @Test
    void shouldDeleteJoinRowsAndKeepGuildAndRole() {
        RelatedUser fixture = persistUserWithRelations();

        given().delete("/api/users/{id}", fixture.userId()).then().statusCode(204);

        assertNull(findUser(fixture.userId()));
        assertEquals(0, relationCount("guild_members", fixture.userId()));
        assertEquals(0, relationCount("user_roles", fixture.userId()));
        assertTrue(guildExists(fixture.guildId()));
        assertTrue(roleExists(fixture.roleId()));
    }

    @Test
    void shouldReturnUniformJsonForMalformedJson() {
        Response response = given()
                .contentType("application/json")
                .body("{")
                .post("/api/users");

        assertError(response, 400, "INVALID_JSON");
        response.then().body("message", equalTo("Invalid JSON structure"));

        Response mismatched = given()
                .contentType("application/json")
                .body(Map.of("username", Set.of("not-a-string"), "displayName", "Invalid"))
                .post("/api/users");
        assertError(mismatched, 400, "INVALID_JSON");
    }

    @Test
    void shouldExposeUserOperationsInOpenApi() {
        given().accept("application/json").get("/q/openapi")
                .then().statusCode(200)
                .body("paths.'/api/users'.post", not(nullValue()))
                .body("paths.'/api/users'.get", not(nullValue()))
                .body("paths.'/api/users/{id}'.get", not(nullValue()))
                .body("paths.'/api/users/{id}'.put", not(nullValue()))
                .body("paths.'/api/users/{id}'.delete", not(nullValue()));
    }

    private UUID createUser(String username, String displayName) {
        return UUID.fromString(given()
                .contentType("application/json")
                .body(Map.of("username", username, "displayName", displayName))
                .post("/api/users")
                .then().statusCode(201)
                .extract().path("id"));
    }

    private UUID persistUser(String username, String displayName, LocalDateTime joinedAt) {
        AtomicReference<UUID> id = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> {
            UserEntity user = new UserEntity(username, displayName, joinedAt);
            userRepository.persistAndFlush(user);
            id.set(user.getId());
        });
        return id.get();
    }

    private void insertUser(UUID id, String username, LocalDateTime joinedAt) {
        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                        "INSERT INTO users (id, username, display_name, joined_at) "
                                + "VALUES (:id, :username, :displayName, :joinedAt)")
                .setParameter("id", id)
                .setParameter("username", username)
                .setParameter("displayName", username)
                .setParameter("joinedAt", joinedAt)
                .executeUpdate());
    }

    private RelatedUser persistUserWithRelations() {
        AtomicReference<RelatedUser> result = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> {
            LocalDateTime joinedAt = LocalDateTime.of(2026, 4, 1, 12, 30);
            GuildEntity guild = new GuildEntity(
                    "User CRUD Guild", "Deletion relation fixture", joinedAt.minusDays(1));
            guildRepository.persist(guild);

            RoleEntity role = new RoleEntity("Member", "#00AA00", 0);
            role.setGuild(guild);
            guild.getRoles().add(role);
            roleRepository.persist(role);

            UserEntity user = new UserEntity("related-user", "Related User", joinedAt);
            user.getGuilds().add(guild);
            user.getRoles().add(role);
            guild.getMembers().add(user);
            role.getUsers().add(user);
            userRepository.persistAndFlush(user);

            result.set(new RelatedUser(user.getId(), guild.getId(), role.getId(), joinedAt));
        });
        return result.get();
    }

    private UserEntity findUser(UUID id) {
        AtomicReference<UserEntity> user = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> user.set(userRepository.findById(id)));
        return user.get();
    }

    private boolean guildExists(UUID id) {
        AtomicReference<Boolean> exists = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(
                () -> exists.set(guildRepository.findById(id) != null));
        return exists.get();
    }

    private boolean roleExists(UUID id) {
        AtomicReference<Boolean> exists = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(
                () -> exists.set(roleRepository.findById(id) != null));
        return exists.get();
    }

    private long relationCount(String table, UUID userId) {
        AtomicLong count = new AtomicLong();
        QuarkusTransaction.requiringNew().run(() -> count.set(((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM " + table + " WHERE user_id = :userId")
                .setParameter("userId", userId)
                .getSingleResult()).longValue()));
        return count.get();
    }

    private static void assertError(Response response, int status, String code) {
        response.then()
                .statusCode(status)
                .contentType("application/json")
                .body("code", equalTo(code))
                .body("message", not(nullValue()))
                .body("timestamp", not(nullValue()));
        LocalDateTime.parse(response.path("timestamp"));
    }

    private record RelatedUser(
            UUID userId,
            UUID guildId,
            UUID roleId,
            LocalDateTime joinedAt) {
    }
}
