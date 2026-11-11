package org.acme.discord.infrastructure.mappingtest;

import org.acme.discord.application.dto.ChannelDTO;
import org.acme.discord.application.dto.usecases.CreateChannelRequest;
import org.acme.discord.application.dto.usecases.UpdateChannelRequest;
import org.acme.discord.domain.ChannelType;
import org.acme.discord.infrastructure.mapper.ChannelMapper;
import org.acme.discord.infrastructure.persistence.ChannelEntity;
import org.acme.discord.infrastructure.persistence.GuildEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChannelMapperTest {

    @Test
    void shouldMapEntityToDtoWithGuildIdentifier() {
        UUID channelId = UUID.randomUUID();
        UUID guildId = UUID.randomUUID();
        GuildEntity guild = guildWithId(guildId);
        ChannelEntity channel = new ChannelEntity("General", ChannelType.TEXT, 1);
        MapperTestSupport.setId(channel, channelId);
        channel.setGuild(guild);

        ChannelDTO dto = ChannelMapper.toDto(channel);

        assertEquals(channelId, dto.id());
        assertEquals("General", dto.name());
        assertEquals(ChannelType.TEXT, dto.type());
        assertEquals(1, dto.position());
        assertEquals(guildId, dto.guildId());
    }

    @Test
    void shouldCreateEntityWithResolvedGuild() {
        UUID guildId = UUID.randomUUID();
        GuildEntity guild = guildWithId(guildId);
        CreateChannelRequest request = new CreateChannelRequest("Voice", ChannelType.VOICE, 2, guildId);

        ChannelEntity entity = ChannelMapper.toEntity(request, guild);

        assertNull(entity.getId());
        assertEquals("Voice", entity.getName());
        assertEquals(ChannelType.VOICE, entity.getType());
        assertEquals(2, entity.getPosition());
        assertSame(guild, entity.getGuild());
    }

    @Test
    void shouldRejectResolvedGuildThatDoesNotMatchRequest() {
        GuildEntity guild = guildWithId(UUID.randomUUID());
        CreateChannelRequest request = new CreateChannelRequest(
                "General", ChannelType.TEXT, 0, UUID.randomUUID());

        assertThrows(IllegalArgumentException.class, () -> ChannelMapper.toEntity(request, guild));
    }

    @Test
    void shouldUpdateEditableFieldsAndPreserveIdentifierAndGuild() {
        UUID channelId = UUID.randomUUID();
        GuildEntity guild = guildWithId(UUID.randomUUID());
        ChannelEntity entity = new ChannelEntity("Before", ChannelType.TEXT, 1);
        MapperTestSupport.setId(entity, channelId);
        entity.setGuild(guild);

        ChannelEntity updated = ChannelMapper.updateEntity(
                new UpdateChannelRequest("After", ChannelType.VOICE, 4), entity);

        assertSame(entity, updated);
        assertEquals(channelId, updated.getId());
        assertSame(guild, updated.getGuild());
        assertEquals("After", updated.getName());
        assertEquals(ChannelType.VOICE, updated.getType());
        assertEquals(4, updated.getPosition());
    }

    private static GuildEntity guildWithId(UUID id) {
        GuildEntity guild = new GuildEntity();
        MapperTestSupport.setId(guild, id);
        return guild;
    }
}
