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
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(PostgreSqlTestResource.class)
class ChannelResourceIT {

    @Inject
    EntityManager entityManager;

    @Inject
    ChannelRepository channelRepository;

    @Inject
    GuildRepository guildRepository;

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
    void shouldCreateTextAndVoiceChannelsWithLocationAndGuild() {
        UUID guildId = persistGuild("Channel Guild");

        UUID textId = createChannel("general", "TEXT", 0, guildId);
        UUID voiceId = createChannel("voice", "VOICE", 1, guildId);

        assertTrue(channelExists(textId));
        assertTrue(channelExists(voiceId));
        assertEquals(guildId, channelGuildId(textId));
        assertEquals(guildId, channelGuildId(voiceId));
        assertEquals(2, channelsInGuild(guildId));
    }

    @Test
    void shouldGenerateIdentifierAndReturnCorrectLocation() {
        UUID guildId = persistGuild("Location Guild");

        Response response = given().contentType("application/json")
                .body(createRequest("announcements", "TEXT", 2, guildId))
                .post("/api/channels")
                .then().statusCode(201)
                .contentType("application/json")
                .body("name", equalTo("announcements"))
                .body("type", equalTo("TEXT"))
                .body("position", equalTo(2))
                .body("guildId", equalTo(guildId.toString()))
                .extract().response();

        UUID channelId = UUID.fromString(response.path("id"));
        assertTrue(response.header("Location").endsWith("/api/channels/" + channelId));
        assertTrue(channelExists(channelId));
    }

    @Test
    void shouldAcceptDuplicateNamesAndPositions() {
        UUID guildId = persistGuild("Duplicate Channels");

        createChannel("duplicate", "TEXT", 1, guildId);
        createChannel("duplicate", "TEXT", 1, guildId);

        assertEquals(2, channelsInGuild(guildId));
    }

    @Test
    void shouldRejectInvalidCreateFields() {
        UUID guildId = persistGuild("Validation Guild");

        assertError(given().contentType("application/json")
                        .body("{\"guildId\":\"" + guildId + "\"}")
                        .post("/api/channels"),
                400, "VALIDATION_ERROR");
        assertError(given().contentType("application/json")
                        .body("{\"name\":null,\"type\":null,\"position\":null,\"guildId\":null}")
                        .post("/api/channels"),
                400, "VALIDATION_ERROR");
        assertError(given().contentType("application/json")
                        .body(createRequest(" ", "TEXT", -1, guildId))
                        .post("/api/channels"),
                400, "VALIDATION_ERROR");
    }

    @Test
    void shouldRejectMalformedOrMissingGuild() {
        assertError(given().contentType("application/json")
                        .body("{\"name\":\"general\",\"type\":\"TEXT\",\"position\":0,"
                                + "\"guildId\":\"not-a-uuid\"}")
                        .post("/api/channels"),
                400, "INVALID_JSON");
        assertError(given().contentType("application/json")
                        .body(createRequest("general", "TEXT", 0, UUID.randomUUID()))
                        .post("/api/channels"),
                404, "GUILD_NOT_FOUND");
    }

    @Test
    void shouldRejectUnknownLowercaseAndNumericChannelTypesOnCreate() {
        UUID guildId = persistGuild("Strict Type Guild");

        assertInvalidTypeOnCreate("\"VIDEO\"", guildId);
        assertInvalidTypeOnCreate("\"text\"", guildId);
        assertInvalidTypeOnCreate("0", guildId);
        assertInvalidTypeOnCreate("1", guildId);
    }

    @Test
    void shouldGetExistingChannelAndRejectMissingOrMalformedIds() {
        UUID guildId = persistGuild("Lookup Guild");
        UUID channelId = persistChannel("lookup", ChannelType.TEXT, 3, guildId);

        given().get("/api/channels/{id}", channelId)
                .then().statusCode(200)
                .body("id", equalTo(channelId.toString()))
                .body("guildId", equalTo(guildId.toString()));

        assertError(given().get("/api/channels/{id}", UUID.randomUUID()),
                404, "CHANNEL_NOT_FOUND");
        assertError(given().get("/api/channels/not-a-uuid"),
                400, "INVALID_REQUEST");
    }

    @Test
    void shouldReturnEmptyDefaultPage() {
        given().get("/api/channels")
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
        insertChannel(firstTie, "First tie", "TEXT", 1, guildId);
        insertChannel(secondTie, "Second tie", "VOICE", 1, guildId);
        insertChannel(later, "Later", "TEXT", 2, guildId);

        given().queryParam("page", 0).queryParam("size", 2).get("/api/channels")
                .then().statusCode(200)
                .body("items", hasSize(2))
                .body("items[0].id", equalTo(firstTie.toString()))
                .body("items[1].id", equalTo(secondTie.toString()))
                .body("totalItems", equalTo(3))
                .body("totalPages", equalTo(2));

        given().queryParam("page", 1).queryParam("size", 2).get("/api/channels")
                .then().statusCode(200)
                .body("items", hasSize(1))
                .body("items[0].id", equalTo(later.toString()));

        given().queryParam("page", 2).queryParam("size", 2).get("/api/channels")
                .then().statusCode(200)
                .body("items", hasSize(0))
                .body("totalItems", equalTo(3))
                .body("totalPages", equalTo(2));
    }

    @Test
    void shouldRejectInvalidPagination() {
        assertError(given().queryParam("page", -1).get("/api/channels"),
                400, "INVALID_REQUEST");
        assertError(given().queryParam("size", 0).get("/api/channels"),
                400, "INVALID_REQUEST");
        assertError(given().queryParam("size", 101).get("/api/channels"),
                400, "INVALID_REQUEST");
        assertError(given().queryParam("page", "abc").get("/api/channels"),
                400, "INVALID_REQUEST");
    }

    @Test
    void shouldUpdateEditableFieldsAndPreserveIdentifierAndGuild() {
        UUID guildId = persistGuild("Update Guild");
        UUID otherGuildId = persistGuild("Other Guild");
        UUID channelId = persistChannel("before", ChannelType.TEXT, 1, guildId);

        given().contentType("application/json")
                .body(Map.of("name", "after", "type", "VOICE", "position", 4,
                        "guildId", otherGuildId.toString()))
                .put("/api/channels/{id}", channelId)
                .then().statusCode(200)
                .body("id", equalTo(channelId.toString()))
                .body("name", equalTo("after"))
                .body("type", equalTo("VOICE"))
                .body("position", equalTo(4))
                .body("guildId", equalTo(guildId.toString()));

        assertEquals(guildId, channelGuildId(channelId));

        given().contentType("application/json")
                .body(Map.of("name", "again", "type", "TEXT", "position", 5))
                .put("/api/channels/{id}", channelId)
                .then().statusCode(200)
                .body("type", equalTo("TEXT"))
                .body("guildId", equalTo(guildId.toString()));
    }

    @Test
    void shouldRejectInvalidTypesOnUpdate() {
        UUID guildId = persistGuild("Update Type Guild");
        UUID channelId = persistChannel("before", ChannelType.TEXT, 1, guildId);

        assertInvalidTypeOnUpdate("\"VIDEO\"", channelId);
        assertInvalidTypeOnUpdate("\"text\"", channelId);
        assertInvalidTypeOnUpdate("0", channelId);
        assertInvalidTypeOnUpdate("1", channelId);
        assertError(given().contentType("application/json")
                        .body("{\"name\":\"after\",\"type\":null,\"position\":1}")
                        .put("/api/channels/{id}", channelId),
                400, "VALIDATION_ERROR");
    }

    @Test
    void shouldRejectInvalidOrMissingChannelUpdate() {
        UUID guildId = persistGuild("Invalid Update Guild");
        UUID channelId = persistChannel("before", ChannelType.TEXT, 1, guildId);

        assertError(given().contentType("application/json")
                        .body(Map.of("name", " ", "type", "TEXT", "position", -1))
                        .put("/api/channels/{id}", channelId),
                400, "VALIDATION_ERROR");
        assertError(given().contentType("application/json")
                        .body(Map.of("name", "missing", "type", "TEXT", "position", 1))
                        .put("/api/channels/{id}", UUID.randomUUID()),
                404, "CHANNEL_NOT_FOUND");
    }

    @Test
    void shouldDeleteChannelAndPreserveGuildAndOtherChannels() {
        UUID guildId = persistGuild("Deletion Guild");
        UUID deletedId = persistChannel("delete", ChannelType.TEXT, 1, guildId);
        UUID preservedId = persistChannel("preserve", ChannelType.VOICE, 2, guildId);

        Response response = given().delete("/api/channels/{id}", deletedId);
        response.then().statusCode(204);
        assertEquals("", response.asString());

        assertFalse(channelExists(deletedId));
        assertTrue(channelExists(preservedId));
        assertTrue(guildExists(guildId));
        assertEquals(1, channelsInGuild(guildId));
    }

    @Test
    void shouldPreventDeletionOfGuildContainingCreatedChannel() {
        UUID guildId = persistGuild("Protected Guild");
        UUID channelId = createChannel("protected", "TEXT", 0, guildId);

        assertError(given().delete("/api/guilds/{id}", guildId),
                409, "GUILD_NOT_EMPTY");

        assertTrue(guildExists(guildId));
        assertTrue(channelExists(channelId));
        assertEquals(1, channelsInGuild(guildId));
    }

    @Test
    void shouldRejectMissingOrMalformedChannelDeletion() {
        assertError(given().delete("/api/channels/{id}", UUID.randomUUID()),
                404, "CHANNEL_NOT_FOUND");
        assertError(given().delete("/api/channels/not-a-uuid"),
                400, "INVALID_REQUEST");
    }

    @Test
    void shouldExposeChannelOperationsInOpenApi() {
        given().accept("application/json").get("/q/openapi")
                .then().statusCode(200)
                .body("paths.'/api/channels'.post", not(nullValue()))
                .body("paths.'/api/channels'.get", not(nullValue()))
                .body("paths.'/api/channels/{id}'.get", not(nullValue()))
                .body("paths.'/api/channels/{id}'.put", not(nullValue()))
                .body("paths.'/api/channels/{id}'.delete", not(nullValue()));
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

    private UUID createChannel(String name, String type, int position, UUID guildId) {
        Response response = given().contentType("application/json")
                .body(createRequest(name, type, position, guildId))
                .post("/api/channels")
                .then().statusCode(201)
                .extract().response();
        UUID id = UUID.fromString(response.path("id"));
        assertTrue(response.header("Location").endsWith("/api/channels/" + id));
        return id;
    }

    private UUID persistChannel(String name, ChannelType type, int position, UUID guildId) {
        AtomicReference<UUID> id = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> {
            GuildEntity guild = guildRepository.findById(guildId);
            ChannelEntity channel = new ChannelEntity(name, type, position);
            channel.setGuild(guild);
            guild.getChannels().add(channel);
            channelRepository.persistAndFlush(channel);
            id.set(channel.getId());
        });
        return id.get();
    }

    private void insertChannel(UUID id, String name, String type, int position, UUID guildId) {
        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                        "INSERT INTO channels (id, name, type, position, guild_id) "
                                + "VALUES (:id, :name, :type, :position, :guildId)")
                .setParameter("id", id)
                .setParameter("name", name)
                .setParameter("type", type)
                .setParameter("position", position)
                .setParameter("guildId", guildId)
                .executeUpdate());
    }

    private boolean channelExists(UUID id) {
        return entityExists(() -> channelRepository.findById(id) != null);
    }

    private boolean guildExists(UUID id) {
        return entityExists(() -> guildRepository.findById(id) != null);
    }

    private boolean entityExists(java.util.function.BooleanSupplier query) {
        AtomicReference<Boolean> exists = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> exists.set(query.getAsBoolean()));
        return exists.get();
    }

    private UUID channelGuildId(UUID channelId) {
        AtomicReference<UUID> guildId = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> guildId.set(
                channelRepository.findById(channelId).getGuild().getId()));
        return guildId.get();
    }

    private long channelsInGuild(UUID guildId) {
        AtomicReference<Long> count = new AtomicReference<>();
        QuarkusTransaction.requiringNew().run(() -> count.set(((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM channels WHERE guild_id = :id")
                .setParameter("id", guildId)
                .getSingleResult()).longValue()));
        return count.get();
    }

    private static Map<String, Object> createRequest(
            String name,
            String type,
            int position,
            UUID guildId) {
        return Map.of("name", name, "type", type, "position", position, "guildId", guildId.toString());
    }

    private static void assertInvalidTypeOnCreate(String jsonValue, UUID guildId) {
        assertError(given().contentType("application/json")
                        .body(channelJson(jsonValue, guildId))
                        .post("/api/channels"),
                400, "INVALID_JSON");
    }

    private static void assertInvalidTypeOnUpdate(String jsonValue, UUID channelId) {
        assertError(given().contentType("application/json")
                        .body("{\"name\":\"after\",\"type\":" + jsonValue + ",\"position\":1}")
                        .put("/api/channels/{id}", channelId),
                400, "INVALID_JSON");
    }

    private static String channelJson(String typeValue, UUID guildId) {
        return "{\"name\":\"general\",\"type\":" + typeValue + ",\"position\":0,"
                + "\"guildId\":\"" + guildId + "\"}";
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
}
