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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
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
class RoleResourceIT {

    @Inject
    EntityManager entityManager;

    @Inject
    RoleRepository roleRepository;

    @Inject
    GuildRepository guildRepository;

    @Inject
    UserRepository userRepository;

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
    void shouldCreateRoleInExistingGuildWithLocation() {
        UUID guildId = persistGuild("Role Guild");

        Response response = given().contentType("application/json")
                .body(createRequest("Moderator", "red", 1, guildId))
                .post("/api/roles")
                .then().statusCode(201)
                .contentType("application/json")
                .body("name", equalTo("Moderator"))
                .body("color", equalTo("red"))
                .body("position", equalTo(1))
                .body("guildId", equalTo(guildId.toString()))
                .extract().response();

        UUID roleId = UUID.fromString(response.path("id"));
        assertTrue(response.header("Location").endsWith("/api/roles/" + roleId));
        assertTrue(roleExists(roleId));
        assertEquals(guildId, roleGuildId(roleId));
        assertEquals(1, rolesInGuild(guildId));
    }

    @Test
    void shouldAcceptDuplicateNamesAndPositionsWithoutColorFormatValidation() {
        UUID guildId = persistGuild("Duplicate Roles");

        createRole("Duplicate", "not-a-hex-color", 2, guildId);
        createRole("Duplicate", "not-a-hex-color", 2, guildId);

        given().get("/api/roles")
                .then().statusCode(200)
                .body("totalItems", equalTo(2));
        assertEquals(2, rolesInGuild(guildId));
    }

    @Test
    void shouldRejectInvalidCreateRequestFields() {
        UUID guildId = persistGuild("Validation Guild");

        assertError(given().contentType("application/json")
                        .body("{\"guildId\":\"" + guildId + "\"}")
                        .post("/api/roles"),
                400, "VALIDATION_ERROR");
        assertError(given().contentType("application/json")
                        .body("{\"name\":null,\"color\":null,\"position\":null,\"guildId\":null}")
                        .post("/api/roles"),
                400, "VALIDATION_ERROR");
        assertError(given().contentType("application/json")
                        .body(createRequest(" ", "", -1, guildId))
                        .post("/api/roles"),
                400, "VALIDATION_ERROR");
    }

    @Test
    void shouldRejectMalformedOrMissingGuild() {
        assertError(given().contentType("application/json")
                        .body("{\"name\":\"Role\",\"color\":\"blue\",\"position\":0,"
                                + "\"guildId\":\"not-a-uuid\"}")
                        .post("/api/roles"),
                400, "INVALID_JSON");

        assertError(given().contentType("application/json")
                        .body(createRequest("Role", "blue", 0, UUID.randomUUID()))
                        .post("/api/roles"),
                404, "GUILD_NOT_FOUND");
    }

    @Test
    void shouldGetExistingRoleAndRejectMissingOrMalformedIds() {
        UUID guildId = persistGuild("Lookup Guild");
        UUID roleId = persistRole("Lookup", "blue", 3, guildId);

        given().get("/api/roles/{id}", roleId)
                .then().statusCode(200)
                .body("id", equalTo(roleId.toString()))
                .body("guildId", equalTo(guildId.toString()));

        assertError(given().get("/api/roles/{id}", UUID.randomUUID()),
                404, "ROLE_NOT_FOUND");
        assertError(given().get("/api/roles/not-a-uuid"),
                400, "INVALID_REQUEST");
    }

    @Test
    void shouldReturnEmptyDefaultPage() {
        given().get("/api/roles")
                .then().statusCode(200)
                .body("items", hasSize(0))
                .body("page", equalTo(0))
                .body("size", equalTo(20))
                .body("totalItems", equalTo(0))
                .body("totalPages", equalTo(0));
    }

    @Test
    void shouldPaginateUsingStablePositionAndIdentifierOrdering() {
        UUID guildId = persistGuild("Pagination Guild");
        UUID firstTie = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID secondTie = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID later = UUID.fromString("00000000-0000-0000-0000-000000000001");
        insertRole(firstTie, "First tie", 1, guildId);
        insertRole(secondTie, "Second tie", 1, guildId);
        insertRole(later, "Later", 2, guildId);

        given().queryParam("page", 0).queryParam("size", 2).get("/api/roles")
                .then().statusCode(200)
                .body("items", hasSize(2))
                .body("items[0].id", equalTo(firstTie.toString()))
                .body("items[1].id", equalTo(secondTie.toString()))
                .body("totalItems", equalTo(3))
                .body("totalPages", equalTo(2));

        given().queryParam("page", 1).queryParam("size", 2).get("/api/roles")
                .then().statusCode(200)
                .body("items", hasSize(1))
                .body("items[0].id", equalTo(later.toString()));

        given().queryParam("page", 2).queryParam("size", 2).get("/api/roles")
                .then().statusCode(200)
                .body("items", hasSize(0))
                .body("totalItems", equalTo(3))
                .body("totalPages", equalTo(2));
    }

    @Test
    void shouldRejectInvalidPagination() {
        assertError(given().queryParam("page", -1).get("/api/roles"),
                400, "INVALID_REQUEST");
        assertError(given().queryParam("size", 0).get("/api/roles"),
                400, "INVALID_REQUEST");
        assertError(given().queryParam("size", 101).get("/api/roles"),
                400, "INVALID_REQUEST");
        assertError(given().queryParam("page", "abc").get("/api/roles"),
                400, "INVALID_REQUEST");
    }

    @Test
    void shouldUpdateEditableFieldsAndPreserveIdentifierGuildAndAssignments() {
        AssignedRole fixture = persistAssignedRole();

        Response response = given().contentType("application/json")
                .body(Map.of("name", "Updated", "color", "green", "position", 5,
                        "guildId", UUID.randomUUID().toString()))
                .put("/api/roles/{id}", fixture.roleId())
                .then().statusCode(200)
                .body("id", equalTo(fixture.roleId().toString()))
                .body("name", equalTo("Updated"))
                .body("color", equalTo("green"))
                .body("position", equalTo(5))
                .body("guildId", equalTo(fixture.guildId().toString()))
                .extract().response();

        assertNotNull(response.path("id"));
        assertEquals(fixture.guildId(), roleGuildId(fixture.roleId()));
        assertEquals(1, assignmentsForRole(fixture.roleId()));
        assertTrue(userExists(fixture.userId()));
    }

    @Test
    void shouldRejectInvalidOrMissingRoleUpdate() {
        UUID guildId = persistGuild("Update Guild");
        UUID roleId = persistRole("Before", "black", 0, guildId);

        assertError(given().contentType("application/json")
                        .body(Map.of("name", "", "color", " ", "position", -1))
                        .put("/api/roles/{id}", roleId),
                400, "VALIDATION_ERROR");
        assertError(given().contentType("application/json")
                        .body(Map.of("name", "Missing", "color", "blue", "position", 1))
                        .put("/api/roles/{id}", UUID.randomUUID()),
                404, "ROLE_NOT_FOUND");
    }

    @Test
    void shouldDeleteUnassignedRoleAndPreserveGuildAndOtherRoles() {
        UUID guildId = persistGuild("Deletion Guild");
        UUID deletedRole = persistRole("Delete", "red", 1, guildId);
        UUID preservedRole = persistRole("Preserve", "blue", 2, guildId);

        Response response = given().delete("/api/roles/{id}", deletedRole);
        response.then().statusCode(204);
        assertEquals("", response.asString());

        assertFalse(roleExists(deletedRole));
        assertTrue(roleExists(preservedRole));
        assertTrue(guildExists(guildId));
        assertEquals(1, rolesInGuild(guildId));
    }

    @Test
    void shouldRejectAssignedRoleDeletionAndPreserveAllData() {
        AssignedRole fixture = persistAssignedRole();

        Response response = given().delete("/api/roles/{id}", fixture.roleId());
        assertError(response, 409, "ROLE_IN_USE");

        assertTrue(roleExists(fixture.roleId()));
        assertTrue(userExists(fixture.userId()));
        assertTrue(guildExists(fixture.guildId()));
        assertEquals(1, assignmentsForRole(fixture.roleId()));
    }

    @Test
    void shouldRejectMissingOrMalformedRoleDeletion() {
        assertError(given().delete("/api/roles/{id}", UUID.randomUUID()),
                404, "ROLE_NOT_FOUND");
        assertError(given().delete("/api/roles/not-a-uuid"),
                400, "INVALID_REQUEST");
    }

    @Test
    void shouldExposeRoleOperationsInOpenApi() {
        given().accept("application/json").get("/q/openapi")
                .then().statusCode(200)
                .body("paths.'/api/roles'.post", not(nullValue()))
                .body("paths.'/api/roles'.get", not(nullValue()))
                .body("paths.'/api/roles/{id}'.get", not(nullValue()))
                .body("paths.'/api/roles/{id}'.put", not(nullValue()))
                .body("paths.'/api/roles/{id}'.delete", not(nullValue()));
    }

    private UUID persistGuild(String name) {
        AtomicReference<UUID> id = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> {
            GuildEntity guild = new GuildEntity(name, null, LocalDateTime.of(2026, 9, 1, 10, 0));
            guildRepository.persistAndFlush(guild);
            id.set(guild.getId());
        });
        return id.get();
    }

    private UUID createRole(String name, String color, int position, UUID guildId) {
        return UUID.fromString(given().contentType("application/json")
                .body(createRequest(name, color, position, guildId))
                .post("/api/roles")
                .then().statusCode(201)
                .extract().path("id"));
    }

    private UUID persistRole(String name, String color, int position, UUID guildId) {
        AtomicReference<UUID> id = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> {
            GuildEntity guild = guildRepository.findById(guildId);
            RoleEntity role = new RoleEntity(name, color, position);
            role.setGuild(guild);
            guild.getRoles().add(role);
            roleRepository.persistAndFlush(role);
            id.set(role.getId());
        });
        return id.get();
    }

    private void insertRole(UUID id, String name, int position, UUID guildId) {
        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                        "INSERT INTO roles (id, name, color, position, guild_id) "
                                + "VALUES (:id, :name, 'blue', :position, :guildId)")
                .setParameter("id", id)
                .setParameter("name", name)
                .setParameter("position", position)
                .setParameter("guildId", guildId)
                .executeUpdate());
    }

    private AssignedRole persistAssignedRole() {
        AtomicReference<AssignedRole> result = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> {
            GuildEntity guild = new GuildEntity(
                    "Assigned Role Guild", null, LocalDateTime.of(2026, 10, 1, 10, 0));
            guildRepository.persist(guild);

            RoleEntity role = new RoleEntity("Assigned", "purple", 1);
            role.setGuild(guild);
            guild.getRoles().add(role);
            roleRepository.persist(role);

            UserEntity user = new UserEntity(
                    "role-user", "Role User", LocalDateTime.of(2026, 10, 2, 10, 0));
            user.getRoles().add(role);
            role.getUsers().add(user);
            userRepository.persistAndFlush(user);

            result.set(new AssignedRole(role.getId(), guild.getId(), user.getId()));
        });
        return result.get();
    }

    private boolean roleExists(UUID id) {
        return entityExists(() -> roleRepository.findById(id) != null);
    }

    private boolean guildExists(UUID id) {
        return entityExists(() -> guildRepository.findById(id) != null);
    }

    private boolean userExists(UUID id) {
        return entityExists(() -> userRepository.findById(id) != null);
    }

    private boolean entityExists(java.util.function.BooleanSupplier query) {
        AtomicReference<Boolean> exists = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> exists.set(query.getAsBoolean()));
        return exists.get();
    }

    private UUID roleGuildId(UUID roleId) {
        AtomicReference<UUID> guildId = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> guildId.set(
                roleRepository.findById(roleId).getGuild().getId()));
        return guildId.get();
    }

    private long rolesInGuild(UUID guildId) {
        return count("SELECT COUNT(*) FROM roles WHERE guild_id = :id", guildId);
    }

    private long assignmentsForRole(UUID roleId) {
        return count("SELECT COUNT(*) FROM user_roles WHERE role_id = :id", roleId);
    }

    private long count(String sql, UUID id) {
        AtomicReference<Long> count = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> count.set(((Number) entityManager
                .createNativeQuery(sql)
                .setParameter("id", id)
                .getSingleResult()).longValue()));
        return count.get();
    }

    private static Map<String, Object> createRequest(
            String name,
            String color,
            int position,
            UUID guildId) {
        return Map.of("name", name, "color", color, "position", position, "guildId", guildId.toString());
    }

    private static void assertError(Response response, int status, String code) {
        response.then()
                .statusCode(status)
                .contentType("application/json")
                .body("code", equalTo(code))
                .body("message", not(nullValue()))
                .body("timestamp", not(nullValue()));
        LocalDateTime.parse(response.path("timestamp"));
        assertFalse(response.asString().toLowerCase().contains("sql"));
    }

    private record AssignedRole(UUID roleId, UUID guildId, UUID userId) {
    }
}
