package org.acme.catalog.product.infrastructure.persistence;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.acme.catalog.product.domain.ChannelType;
import org.acme.testresources.PostgreSqlTestResource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Discord persistence mappings against PostgreSQL.
 */
@QuarkusTest
@QuarkusTestResource(PostgreSqlTestResource.class)
class DiscordPersistenceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    GuildRepository guildRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    ChannelRepository channelRepository;

    @Inject
    EntityManager entityManager;

    @Test
    @TestTransaction
    void shouldPersistAndReloadUserAndGuild() {
        LocalDateTime joinedAt = LocalDateTime.of(2026, 1, 10, 9, 30);
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 11, 10, 45);
        UserEntity user = new UserEntity("persistence-user", "Persistence User", joinedAt);
        GuildEntity guild = new GuildEntity("Persistence Guild", "Guild used by persistence tests", createdAt);

        userRepository.persist(user);
        guildRepository.persist(guild);

        assertNotNull(user.getId());
        assertNotNull(guild.getId());
        UUID userId = user.getId();
        UUID guildId = guild.getId();

        entityManager.flush();
        entityManager.clear();

        UserEntity reloadedUser = userRepository.findById(userId);
        GuildEntity reloadedGuild = guildRepository.findById(guildId);

        assertNotNull(reloadedUser);
        assertEquals("persistence-user", reloadedUser.getUsername());
        assertEquals("Persistence User", reloadedUser.getDisplayName());
        assertEquals(joinedAt, reloadedUser.getJoinedAt());

        assertNotNull(reloadedGuild);
        assertEquals("Persistence Guild", reloadedGuild.getName());
        assertEquals("Guild used by persistence tests", reloadedGuild.getDescription());
        assertEquals(createdAt, reloadedGuild.getCreatedAt());
    }

    @Test
    @TestTransaction
    void shouldPersistAndReloadGuildRoleAndVoiceChannelRelations() {
        GuildEntity guild = new GuildEntity(
                "Relations Guild",
                "Guild containing a role and a channel",
                LocalDateTime.of(2026, 2, 1, 8, 0));
        guildRepository.persist(guild);

        RoleEntity role = new RoleEntity("Moderator", "#3366FF", 1);
        role.setGuild(guild);
        guild.getRoles().add(role);

        ChannelEntity channel = new ChannelEntity("General Voice", ChannelType.VOICE, 2);
        channel.setGuild(guild);
        guild.getChannels().add(channel);

        roleRepository.persist(role);
        channelRepository.persist(channel);

        assertNotNull(guild.getId());
        assertNotNull(role.getId());
        assertNotNull(channel.getId());
        UUID guildId = guild.getId();
        UUID roleId = role.getId();
        UUID channelId = channel.getId();

        entityManager.flush();
        entityManager.clear();

        RoleEntity reloadedRole = roleRepository.findById(roleId);
        ChannelEntity reloadedChannel = channelRepository.findById(channelId);
        GuildEntity reloadedGuild = guildRepository.findById(guildId);

        assertNotNull(reloadedRole);
        assertEquals(guildId, reloadedRole.getGuild().getId());

        assertNotNull(reloadedChannel);
        assertEquals(guildId, reloadedChannel.getGuild().getId());
        assertEquals(ChannelType.VOICE, reloadedChannel.getType());

        assertNotNull(reloadedGuild);
        assertTrue(reloadedGuild.getRoles().stream()
                .anyMatch(reloaded -> roleId.equals(reloaded.getId())));
        assertTrue(reloadedGuild.getChannels().stream()
                .anyMatch(reloaded -> channelId.equals(reloaded.getId())));
    }

    @Test
    @TestTransaction
    void shouldPersistAndReloadUserGuildMembershipAndRoleAssignment() {
        GuildEntity guild = new GuildEntity(
                "Membership Guild",
                null,
                LocalDateTime.of(2026, 3, 1, 12, 0));
        guildRepository.persist(guild);

        RoleEntity role = new RoleEntity("Member", "#00AA00", 0);
        role.setGuild(guild);
        guild.getRoles().add(role);
        roleRepository.persist(role);

        UserEntity user = new UserEntity(
                "membership-user",
                "Membership User",
                LocalDateTime.of(2026, 3, 2, 13, 15));
        user.getGuilds().add(guild);
        user.getRoles().add(role);
        guild.getMembers().add(user);
        role.getUsers().add(user);
        userRepository.persist(user);

        assertNotNull(user.getId());
        assertNotNull(guild.getId());
        assertNotNull(role.getId());
        UUID userId = user.getId();
        UUID guildId = guild.getId();
        UUID roleId = role.getId();

        entityManager.flush();
        entityManager.clear();

        UserEntity reloadedUser = userRepository.findById(userId);
        assertNotNull(reloadedUser);
        assertTrue(reloadedUser.getGuilds().stream()
                .anyMatch(reloaded -> guildId.equals(reloaded.getId())));
        assertTrue(reloadedUser.getRoles().stream()
                .anyMatch(reloaded -> roleId.equals(reloaded.getId())));

        GuildEntity reloadedGuild = guildRepository.findById(guildId);
        RoleEntity reloadedRole = roleRepository.findById(roleId);

        assertNotNull(reloadedGuild);
        assertTrue(reloadedGuild.getMembers().stream()
                .anyMatch(reloaded -> userId.equals(reloaded.getId())));

        assertNotNull(reloadedRole);
        assertTrue(reloadedRole.getUsers().stream()
                .anyMatch(reloaded -> userId.equals(reloaded.getId())));
    }
}
