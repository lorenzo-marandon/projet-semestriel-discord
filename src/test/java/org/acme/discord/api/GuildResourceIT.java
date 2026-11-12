package org.acme.discord.api;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.acme.discord.domain.ChannelType;
import org.acme.discord.infrastructure.persistence.ChannelEntity;
import org.acme.discord.infrastructure.persistence.ChannelRepository;
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
class GuildResourceIT {

    @Inject
    EntityManager entityManager;

    @Inject
    GuildRepository guildRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    ChannelRepository channelRepository;

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
    void shouldCreateGuildWithLocationAndServerManagedFields() {
        Instant before = Instant.now();

        Response response = given()
                .contentType("application/json")
                .body(Map.of("name", "Community", "description", "A test guild"))
                .post("/api/guilds")
                .then().statusCode(201)
                .contentType("application/json")
                .body("name", equalTo("Community"))
                .body("description", equalTo("A test guild"))
                .body("memberIds", hasSize(0))
                .body("roleIds", hasSize(0))
                .body("channelIds", hasSize(0))
                .extract().response();

        Instant after = Instant.now();
        UUID id = UUID.fromString(response.path("id"));
        LocalDateTime createdAt = LocalDateTime.parse(response.path("createdAt"));
        Instant generated = createdAt.toInstant(ZoneOffset.UTC);

        assertTrue(response.header("Location").endsWith("/api/guilds/" + id));
        assertFalse(generated.isBefore(before.minusSeconds(1)));
        assertFalse(generated.isAfter(after.plusSeconds(1)));
        assertNotNull(findGuild(id));
    }

    @Test
    void shouldAllowNullOrMissingDescriptionAndDuplicateNames() {
        createGuild("Same Name", null);
        createGuild("Same Name", null);

        given().contentType("application/json")
                .body(Map.of("name", "Missing Description"))
                .post("/api/guilds")
                .then().statusCode(201)
                .body("description", nullValue());

        given().get("/api/guilds")
                .then().statusCode(200)
                .body("totalItems", equalTo(3));
    }

    @Test
    void shouldRejectInvalidNamesAndMalformedJson() {
        assertError(given().contentType("application/json").body("{}").post("/api/guilds"),
                400, "VALIDATION_ERROR");
        assertError(given().contentType("application/json")
                        .body("{\"name\":null,\"description\":null}").post("/api/guilds"),
                400, "VALIDATION_ERROR");
        assertError(given().contentType("application/json")
                        .body(Map.of("name", "", "description", "invalid")).post("/api/guilds"),
                400, "VALIDATION_ERROR");
        assertError(given().contentType("application/json")
                        .body(Map.of("name", "   ", "description", "invalid")).post("/api/guilds"),
                400, "VALIDATION_ERROR");
        assertError(given().contentType("application/json").body("{").post("/api/guilds"),
                400, "INVALID_JSON");
    }

    @Test
    void shouldGetExistingGuildAndRejectMissingOrMalformedIds() {
        UUID id = persistGuild("Lookup", null, LocalDateTime.of(2026, 1, 1, 10, 0));

        given().get("/api/guilds/{id}", id)
                .then().statusCode(200)
                .body("id", equalTo(id.toString()))
                .body("name", equalTo("Lookup"));

        assertError(given().get("/api/guilds/{id}", UUID.randomUUID()),
                404, "GUILD_NOT_FOUND");
        assertError(given().get("/api/guilds/not-a-uuid"),
                400, "INVALID_REQUEST");
    }

    @Test
    void shouldReturnEmptyDefaultPage() {
        given().get("/api/guilds")
                .then().statusCode(200)
                .body("items", hasSize(0))
                .body("page", equalTo(0))
                .body("size", equalTo(20))
                .body("totalItems", equalTo(0))
                .body("totalPages", equalTo(0));
    }

    @Test
    void shouldPaginateUsingStableDatabaseOrdering() {
        LocalDateTime firstDate = LocalDateTime.of(2026, 2, 1, 8, 0);
        UUID firstTie = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID secondTie = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID later = UUID.fromString("00000000-0000-0000-0000-000000000001");
        insertGuild(firstTie, "First tie", firstDate);
        insertGuild(secondTie, "Second tie", firstDate);
        insertGuild(later, "Later", firstDate.plusDays(1));

        given().queryParam("page", 0).queryParam("size", 2).get("/api/guilds")
                .then().statusCode(200)
                .body("items", hasSize(2))
                .body("items[0].id", equalTo(firstTie.toString()))
                .body("items[1].id", equalTo(secondTie.toString()))
                .body("totalItems", equalTo(3))
                .body("totalPages", equalTo(2));

        given().queryParam("page", 1).queryParam("size", 2).get("/api/guilds")
                .then().statusCode(200)
                .body("items", hasSize(1))
                .body("items[0].id", equalTo(later.toString()));

        given().queryParam("page", 2).queryParam("size", 2).get("/api/guilds")
                .then().statusCode(200)
                .body("items", hasSize(0))
                .body("totalItems", equalTo(3))
                .body("totalPages", equalTo(2));
    }

    @Test
    void shouldRejectInvalidPagination() {
        assertError(given().queryParam("page", -1).get("/api/guilds"),
                400, "INVALID_REQUEST");
        assertError(given().queryParam("size", 0).get("/api/guilds"),
                400, "INVALID_REQUEST");
        assertError(given().queryParam("size", 101).get("/api/guilds"),
                400, "INVALID_REQUEST");
        assertError(given().queryParam("page", "abc").get("/api/guilds"),
                400, "INVALID_REQUEST");
    }

    @Test
    void shouldUpdateEditableFieldsAndPreserveServerManagedDataAndRelations() {
        RelatedGuild fixture = persistGuildWithAllRelations();

        Response response = given().contentType("application/json")
                .body("{\"name\":\"Updated Guild\",\"description\":null}")
                .put("/api/guilds/{id}", fixture.guildId())
                .then().statusCode(200)
                .body("id", equalTo(fixture.guildId().toString()))
                .body("name", equalTo("Updated Guild"))
                .body("description", nullValue())
                .body("memberIds", equalTo(Set.of(fixture.userId()).stream()
                        .map(UUID::toString).toList()))
                .body("roleIds", equalTo(Set.of(fixture.roleId()).stream()
                        .map(UUID::toString).toList()))
                .body("channelIds", equalTo(Set.of(fixture.channelId()).stream()
                        .map(UUID::toString).toList()))
                .extract().response();

        assertEquals(fixture.createdAt(), LocalDateTime.parse(response.path("createdAt")));
        assertEquals(1, memberCount(fixture.guildId()));
        assertEquals(1, roleCount(fixture.guildId()));
        assertEquals(1, channelCount(fixture.guildId()));
    }

    @Test
    void shouldRejectInvalidOrMissingGuildUpdate() {
        UUID id = persistGuild("Update", "Before", LocalDateTime.of(2026, 3, 1, 10, 0));

        assertError(given().contentType("application/json")
                        .body(Map.of("name", "", "description", "invalid"))
                        .put("/api/guilds/{id}", id),
                400, "VALIDATION_ERROR");
        assertError(given().contentType("application/json")
                        .body(Map.of("name", "Missing"))
                        .put("/api/guilds/{id}", UUID.randomUUID()),
                404, "GUILD_NOT_FOUND");
    }

    @Test
    void shouldDeleteEmptyGuildAndRejectMissingOrMalformedIds() {
        UUID id = persistGuild("Empty", null, LocalDateTime.of(2026, 4, 1, 10, 0));

        Response response = given().delete("/api/guilds/{id}", id);
        response.then().statusCode(204);
        assertEquals("", response.asString());
        assertNull(findGuild(id));

        assertError(given().delete("/api/guilds/{id}", id),
                404, "GUILD_NOT_FOUND");
        assertError(given().delete("/api/guilds/not-a-uuid"),
                400, "INVALID_REQUEST");
    }

    @Test
    void shouldRejectDeletionWhenGuildHasMemberAndPreserveData() {
        RelationFixture fixture = persistGuildWithMember();

        assertError(given().delete("/api/guilds/{id}", fixture.guildId()),
                409, "GUILD_NOT_EMPTY");

        assertNotNull(findGuild(fixture.guildId()));
        assertTrue(userExists(fixture.relatedId()));
        assertEquals(1, memberCount(fixture.guildId()));
    }

    @Test
    void shouldRejectDeletionWhenGuildHasRoleAndPreserveData() {
        RelationFixture fixture = persistGuildWithRole();

        assertError(given().delete("/api/guilds/{id}", fixture.guildId()),
                409, "GUILD_NOT_EMPTY");

        assertNotNull(findGuild(fixture.guildId()));
        assertTrue(roleExists(fixture.relatedId()));
        assertEquals(1, roleCount(fixture.guildId()));
    }

    @Test
    void shouldRejectDeletionWhenGuildHasChannelAndPreserveData() {
        RelationFixture fixture = persistGuildWithChannel();

        assertError(given().delete("/api/guilds/{id}", fixture.guildId()),
                409, "GUILD_NOT_EMPTY");

        assertNotNull(findGuild(fixture.guildId()));
        assertTrue(channelExists(fixture.relatedId()));
        assertEquals(1, channelCount(fixture.guildId()));
    }

    @Test
    void shouldExposeGuildOperationsInOpenApi() {
        given().accept("application/json").get("/q/openapi")
                .then().statusCode(200)
                .body("paths.'/api/guilds'.post", not(nullValue()))
                .body("paths.'/api/guilds'.get", not(nullValue()))
                .body("paths.'/api/guilds/{id}'.get", not(nullValue()))
                .body("paths.'/api/guilds/{id}'.put", not(nullValue()))
                .body("paths.'/api/guilds/{id}'.delete", not(nullValue()));
    }

    private UUID createGuild(String name, String description) {
        String body = description == null
                ? "{\"name\":\"" + name + "\",\"description\":null}"
                : "{\"name\":\"" + name + "\",\"description\":\"" + description + "\"}";
        return UUID.fromString(given().contentType("application/json")
                .body(body)
                .post("/api/guilds")
                .then().statusCode(201)
                .extract().path("id"));
    }

    private UUID persistGuild(String name, String description, LocalDateTime createdAt) {
        AtomicReference<UUID> id = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> {
            GuildEntity guild = new GuildEntity(name, description, createdAt);
            guildRepository.persistAndFlush(guild);
            id.set(guild.getId());
        });
        return id.get();
    }

    private void insertGuild(UUID id, String name, LocalDateTime createdAt) {
        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                        "INSERT INTO guilds (id, name, description, created_at) "
                                + "VALUES (:id, :name, NULL, :createdAt)")
                .setParameter("id", id)
                .setParameter("name", name)
                .setParameter("createdAt", createdAt)
                .executeUpdate());
    }

    private RelatedGuild persistGuildWithAllRelations() {
        AtomicReference<RelatedGuild> result = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> {
            LocalDateTime createdAt = LocalDateTime.of(2026, 5, 1, 12, 0);
            GuildEntity guild = new GuildEntity("Related", "Before", createdAt);
            guildRepository.persist(guild);

            RoleEntity role = new RoleEntity("Member", "#00AA00", 0);
            role.setGuild(guild);
            guild.getRoles().add(role);
            roleRepository.persist(role);

            ChannelEntity channel = new ChannelEntity("General", ChannelType.TEXT, 0);
            channel.setGuild(guild);
            guild.getChannels().add(channel);
            channelRepository.persist(channel);

            UserEntity user = new UserEntity("guild-update-user", "Guild Update User", createdAt.plusDays(1));
            user.getGuilds().add(guild);
            guild.getMembers().add(user);
            userRepository.persistAndFlush(user);

            result.set(new RelatedGuild(
                    guild.getId(), user.getId(), role.getId(), channel.getId(), createdAt));
        });
        return result.get();
    }

    private RelationFixture persistGuildWithMember() {
        AtomicReference<RelationFixture> result = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> {
            GuildEntity guild = new GuildEntity("Members", null, LocalDateTime.of(2026, 6, 1, 10, 0));
            guildRepository.persist(guild);
            UserEntity user = new UserEntity(
                    "guild-member", "Guild Member", LocalDateTime.of(2026, 6, 2, 10, 0));
            user.getGuilds().add(guild);
            guild.getMembers().add(user);
            userRepository.persistAndFlush(user);
            result.set(new RelationFixture(guild.getId(), user.getId()));
        });
        return result.get();
    }

    private RelationFixture persistGuildWithRole() {
        AtomicReference<RelationFixture> result = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> {
            GuildEntity guild = new GuildEntity("Roles", null, LocalDateTime.of(2026, 7, 1, 10, 0));
            guildRepository.persist(guild);
            RoleEntity role = new RoleEntity("Moderator", "#3366FF", 1);
            role.setGuild(guild);
            guild.getRoles().add(role);
            roleRepository.persistAndFlush(role);
            result.set(new RelationFixture(guild.getId(), role.getId()));
        });
        return result.get();
    }

    private RelationFixture persistGuildWithChannel() {
        AtomicReference<RelationFixture> result = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> {
            GuildEntity guild = new GuildEntity("Channels", null, LocalDateTime.of(2026, 8, 1, 10, 0));
            guildRepository.persist(guild);
            ChannelEntity channel = new ChannelEntity("Voice", ChannelType.VOICE, 1);
            channel.setGuild(guild);
            guild.getChannels().add(channel);
            channelRepository.persistAndFlush(channel);
            result.set(new RelationFixture(guild.getId(), channel.getId()));
        });
        return result.get();
    }

    private GuildEntity findGuild(UUID id) {
        AtomicReference<GuildEntity> guild = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> guild.set(guildRepository.findById(id)));
        return guild.get();
    }

    private boolean userExists(UUID id) {
        return entityExists(() -> userRepository.findById(id) != null);
    }

    private boolean roleExists(UUID id) {
        return entityExists(() -> roleRepository.findById(id) != null);
    }

    private boolean channelExists(UUID id) {
        return entityExists(() -> channelRepository.findById(id) != null);
    }

    private boolean entityExists(java.util.function.BooleanSupplier query) {
        AtomicReference<Boolean> exists = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> exists.set(query.getAsBoolean()));
        return exists.get();
    }

    private long memberCount(UUID guildId) {
        return relationCount("SELECT COUNT(*) FROM guild_members WHERE guild_id = :guildId", guildId);
    }

    private long roleCount(UUID guildId) {
        return relationCount("SELECT COUNT(*) FROM roles WHERE guild_id = :guildId", guildId);
    }

    private long channelCount(UUID guildId) {
        return relationCount("SELECT COUNT(*) FROM channels WHERE guild_id = :guildId", guildId);
    }

    private long relationCount(String query, UUID guildId) {
        AtomicReference<Long> count = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> count.set(((Number) entityManager
                .createNativeQuery(query)
                .setParameter("guildId", guildId)
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
        assertFalse(response.asString().toLowerCase().contains("sql"));
    }

    private record RelatedGuild(
            UUID guildId,
            UUID userId,
            UUID roleId,
            UUID channelId,
            LocalDateTime createdAt) {
    }

    private record RelationFixture(UUID guildId, UUID relatedId) {
    }
}
