package org.acme.discord.infrastructure.mappingtest;

import org.acme.discord.application.dto.RoleDTO;
import org.acme.discord.application.dto.usecases.CreateRoleRequest;
import org.acme.discord.application.dto.usecases.UpdateRoleRequest;
import org.acme.discord.infrastructure.mapper.RoleMapper;
import org.acme.discord.infrastructure.persistence.GuildEntity;
import org.acme.discord.infrastructure.persistence.RoleEntity;
import org.acme.discord.infrastructure.persistence.UserEntity;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleMapperTest {

    @Test
    void shouldMapEntityToDtoWithGuildIdentifier() {
        UUID roleId = UUID.randomUUID();
        UUID guildId = UUID.randomUUID();
        GuildEntity guild = guildWithId(guildId);
        RoleEntity role = new RoleEntity("Moderator", "#3366FF", 2);
        MapperTestSupport.setId(role, roleId);
        role.setGuild(guild);

        RoleDTO dto = RoleMapper.toDto(role);

        assertEquals(roleId, dto.id());
        assertEquals("Moderator", dto.name());
        assertEquals("#3366FF", dto.color());
        assertEquals(2, dto.position());
        assertEquals(guildId, dto.guildId());
    }

    @Test
    void shouldCreateEntityWithResolvedGuild() {
        UUID guildId = UUID.randomUUID();
        GuildEntity guild = guildWithId(guildId);
        CreateRoleRequest request = new CreateRoleRequest("Member", "#00AA00", 0, guildId);

        RoleEntity entity = RoleMapper.toEntity(request, guild);

        assertNull(entity.getId());
        assertEquals("Member", entity.getName());
        assertEquals("#00AA00", entity.getColor());
        assertEquals(0, entity.getPosition());
        assertSame(guild, entity.getGuild());
    }

    @Test
    void shouldRejectResolvedGuildThatDoesNotMatchRequest() {
        GuildEntity guild = guildWithId(UUID.randomUUID());
        CreateRoleRequest request = new CreateRoleRequest("Member", "#00AA00", 0, UUID.randomUUID());

        assertThrows(IllegalArgumentException.class, () -> RoleMapper.toEntity(request, guild));
    }

    @Test
    void shouldUpdateEditableFieldsAndPreserveIdentifierGuildAndUsers() {
        UUID roleId = UUID.randomUUID();
        GuildEntity guild = guildWithId(UUID.randomUUID());
        RoleEntity entity = new RoleEntity("Before", "#000000", 1);
        UserEntity user = new UserEntity();
        MapperTestSupport.setId(entity, roleId);
        entity.setGuild(guild);
        entity.getUsers().add(user);

        RoleEntity updated = RoleMapper.updateEntity(new UpdateRoleRequest("After", "#FFFFFF", 3), entity);

        assertSame(entity, updated);
        assertEquals(roleId, updated.getId());
        assertSame(guild, updated.getGuild());
        assertEquals(Set.of(user), updated.getUsers());
        assertEquals("After", updated.getName());
        assertEquals("#FFFFFF", updated.getColor());
        assertEquals(3, updated.getPosition());
    }

    private static GuildEntity guildWithId(UUID id) {
        GuildEntity guild = new GuildEntity();
        MapperTestSupport.setId(guild, id);
        return guild;
    }
}
