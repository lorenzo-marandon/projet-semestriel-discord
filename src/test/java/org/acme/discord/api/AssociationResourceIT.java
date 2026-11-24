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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(PostgreSqlTestResource.class)
class AssociationResourceIT {

    private static final int CONCURRENT_REQUESTS = 6;

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
    void shouldAddMemberPersistOnceAndListDto() {
        UUID guildId = persistGuild("Members Guild");
        UUID userId = persistUser("member", "Member");

        putMember(guildId, userId).then().statusCode(204).body(equalTo(""));
        putMember(guildId, userId).then().statusCode(204).body(equalTo(""));

        assertEquals(1, countMemberships(guildId, userId));
        given().get("/api/guilds/{guildId}/members", guildId)
                .then().statusCode(200)
                .contentType("application/json")
                .body("", hasSize(1))
                .body("[0].id", equalTo(userId.toString()))
                .body("[0].username", equalTo("member"));
    }

    @Test
    void shouldListEmptyGuildMembers() {
        UUID guildId = persistGuild("Empty Guild");

        given().get("/api/guilds/{guildId}/members", guildId)
                .then().statusCode(200)
                .body("", hasSize(0));
    }

    @Test
    void shouldRemoveMemberIdempotently() {
        UUID guildId = persistGuild("Departure Guild");
        UUID userId = persistUser("departure", "Departure");
        putMember(guildId, userId).then().statusCode(204);

        deleteMember(guildId, userId).then().statusCode(204).body(equalTo(""));
        deleteMember(guildId, userId).then().statusCode(204).body(equalTo(""));

        assertEquals(0, countMemberships(guildId, userId));
    }

    @Test
    void shouldRejectMissingMembershipResources() {
        UUID guildId = persistGuild("Existing Guild");
        UUID userId = persistUser("existing-user", "Existing User");

        assertError(putMember(guildId, UUID.randomUUID()), 404, "USER_NOT_FOUND");
        assertError(putMember(UUID.randomUUID(), userId), 404, "GUILD_NOT_FOUND");
        assertError(given().get("/api/guilds/{guildId}/members", UUID.randomUUID()),
                404, "GUILD_NOT_FOUND");
    }

    @Test
    void shouldRejectMalformedAssociationIdentifiers() {
        assertError(given().put("/api/guilds/not-a-uuid/members/not-a-uuid"),
                400, "INVALID_REQUEST");
        assertError(given().put("/api/users/not-a-uuid/roles/not-a-uuid"),
                400, "INVALID_REQUEST");
    }

    @Test
    void shouldAssignRolePersistOnceAndListDto() {
        UUID guildId = persistGuild("Role Guild");
        UUID userId = persistUser("role-member", "Role Member");
        UUID roleId = persistRole("Moderator", 1, guildId);
        putMember(guildId, userId).then().statusCode(204);

        putRole(userId, roleId).then().statusCode(204).body(equalTo(""));
        putRole(userId, roleId).then().statusCode(204).body(equalTo(""));

        assertEquals(1, countRoleAssignments(userId, roleId));
        given().get("/api/users/{userId}/roles", userId)
                .then().statusCode(200)
                .contentType("application/json")
                .body("", hasSize(1))
                .body("[0].id", equalTo(roleId.toString()))
                .body("[0].guildId", equalTo(guildId.toString()));
    }

    @Test
    void shouldListEmptyUserRoles() {
        UUID userId = persistUser("roleless", "Roleless");

        given().get("/api/users/{userId}/roles", userId)
                .then().statusCode(200)
                .body("", hasSize(0));
    }

    @Test
    void shouldRemoveRoleIdempotently() {
        UUID guildId = persistGuild("Remove Role Guild");
        UUID userId = persistUser("remove-role", "Remove Role");
        UUID roleId = persistRole("Temporary", 1, guildId);
        putMember(guildId, userId).then().statusCode(204);
        putRole(userId, roleId).then().statusCode(204);

        deleteRole(userId, roleId).then().statusCode(204).body(equalTo(""));
        deleteRole(userId, roleId).then().statusCode(204).body(equalTo(""));

        assertEquals(0, countRoleAssignments(userId, roleId));
    }

    @Test
    void shouldRejectMissingRoleAssignmentResources() {
        UUID guildId = persistGuild("Existing Role Guild");
        UUID userId = persistUser("existing-role-user", "Existing Role User");
        UUID roleId = persistRole("Existing", 1, guildId);

        assertError(putRole(UUID.randomUUID(), roleId), 404, "USER_NOT_FOUND");
        assertError(putRole(userId, UUID.randomUUID()), 404, "ROLE_NOT_FOUND");
        assertError(given().get("/api/users/{userId}/roles", UUID.randomUUID()),
                404, "USER_NOT_FOUND");
    }

    @Test
    void shouldRejectRoleWhenUserDoesNotBelongToItsGuild() {
        UUID guildId = persistGuild("Restricted Guild");
        UUID userId = persistUser("outsider", "Outsider");
        UUID roleId = persistRole("Restricted", 1, guildId);

        assertError(putRole(userId, roleId), 409, "USER_NOT_GUILD_MEMBER");
        assertEquals(0, countRoleAssignments(userId, roleId));
    }

    @Test
    void shouldRemoveDepartedGuildRolesAndPreserveOtherGuildRoles() {
        UUID departedGuildId = persistGuild("Departed Guild");
        UUID retainedGuildId = persistGuild("Retained Guild");
        UUID userId = persistUser("multi-guild", "Multi Guild");
        UUID departedRoleId = persistRole("Departed Role", 1, departedGuildId);
        UUID retainedRoleId = persistRole("Retained Role", 2, retainedGuildId);
        putMember(departedGuildId, userId).then().statusCode(204);
        putMember(retainedGuildId, userId).then().statusCode(204);
        putRole(userId, departedRoleId).then().statusCode(204);
        putRole(userId, retainedRoleId).then().statusCode(204);

        deleteMember(departedGuildId, userId).then().statusCode(204);

        assertEquals(0, countMemberships(departedGuildId, userId));
        assertEquals(1, countMemberships(retainedGuildId, userId));
        assertEquals(0, countRoleAssignments(userId, departedRoleId));
        assertEquals(1, countRoleAssignments(userId, retainedRoleId));
    }

    @Test
    void shouldSerializeConcurrentMembershipAdditionsWithoutDuplicates() throws Exception {
        UUID guildId = persistGuild("Concurrent Members Guild");
        UUID userId = persistUser("concurrent-member", "Concurrent Member");

        List<Integer> statuses = runConcurrently(
                () -> putMember(guildId, userId).statusCode());

        assertTrue(statuses.stream().allMatch(status -> status == 204));
        assertEquals(1, countMemberships(guildId, userId));
    }

    @Test
    void shouldSerializeConcurrentRoleAssignmentsWithoutDuplicates() throws Exception {
        UUID guildId = persistGuild("Concurrent Roles Guild");
        UUID userId = persistUser("concurrent-role", "Concurrent Role");
        UUID roleId = persistRole("Concurrent Role", 1, guildId);
        putMember(guildId, userId).then().statusCode(204);

        List<Integer> statuses = runConcurrently(
                () -> putRole(userId, roleId).statusCode());

        assertTrue(statuses.stream().allMatch(status -> status == 204));
        assertEquals(1, countRoleAssignments(userId, roleId));
    }

    @Test
    void shouldExposeAssociationOperationsInOpenApi() {
        given().accept("application/json").get("/q/openapi")
                .then().statusCode(200)
                .body("paths.'/api/guilds/{guildId}/members/{userId}'.put", not(nullValue()))
                .body("paths.'/api/guilds/{guildId}/members/{userId}'.delete", not(nullValue()))
                .body("paths.'/api/guilds/{guildId}/members'.get", not(nullValue()))
                .body("paths.'/api/users/{userId}/roles/{roleId}'.put", not(nullValue()))
                .body("paths.'/api/users/{userId}/roles/{roleId}'.delete", not(nullValue()))
                .body("paths.'/api/users/{userId}/roles'.get", not(nullValue()));
    }

    private UUID persistGuild(String name) {
        AtomicReference<UUID> id = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> {
            GuildEntity guild = new GuildEntity(name, null, LocalDateTime.of(2026, 11, 1, 10, 0));
            guildRepository.persistAndFlush(guild);
            id.set(guild.getId());
        });
        return id.get();
    }

    private UUID persistUser(String username, String displayName) {
        AtomicReference<UUID> id = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> {
            UserEntity user = new UserEntity(
                    username, displayName, LocalDateTime.of(2026, 11, 2, 10, 0));
            userRepository.persistAndFlush(user);
            id.set(user.getId());
        });
        return id.get();
    }

    private UUID persistRole(String name, int position, UUID guildId) {
        AtomicReference<UUID> id = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> {
            GuildEntity guild = guildRepository.findById(guildId);
            RoleEntity role = new RoleEntity(name, "blue", position);
            role.setGuild(guild);
            guild.getRoles().add(role);
            roleRepository.persistAndFlush(role);
            id.set(role.getId());
        });
        return id.get();
    }

    private Response putMember(UUID guildId, UUID userId) {
        return given().contentType("application/json")
                .put("/api/guilds/{guildId}/members/{userId}", guildId, userId);
    }

    private Response deleteMember(UUID guildId, UUID userId) {
        return given().delete("/api/guilds/{guildId}/members/{userId}", guildId, userId);
    }

    private Response putRole(UUID userId, UUID roleId) {
        return given().contentType("application/json")
                .put("/api/users/{userId}/roles/{roleId}", userId, roleId);
    }

    private Response deleteRole(UUID userId, UUID roleId) {
        return given().delete("/api/users/{userId}/roles/{roleId}", userId, roleId);
    }

    private long countMemberships(UUID guildId, UUID userId) {
        return countJoinRows("guild_members", "guild_id", guildId, userId);
    }

    private long countRoleAssignments(UUID userId, UUID roleId) {
        return countJoinRows("user_roles", "role_id", roleId, userId);
    }

    private long countJoinRows(String table, String relatedColumn, UUID relatedId, UUID userId) {
        AtomicReference<Long> count = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> count.set(((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM " + table
                        + " WHERE user_id = :userId AND " + relatedColumn + " = :relatedId")
                .setParameter("userId", userId)
                .setParameter("relatedId", relatedId)
                .getSingleResult()).longValue()));
        return count.get();
    }

    private List<Integer> runConcurrently(StatusRequest request) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int index = 0; index < CONCURRENT_REQUESTS; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    return request.execute();
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(20, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static void assertError(Response response, int status, String code) {
        response.then()
                .statusCode(status)
                .contentType("application/json")
                .body("code", equalTo(code))
                .body("message", not(nullValue()))
                .body("timestamp", not(nullValue()))
                .body("violations", not(nullValue()));
    }

    @FunctionalInterface
    private interface StatusRequest {
        int execute();
    }
}
