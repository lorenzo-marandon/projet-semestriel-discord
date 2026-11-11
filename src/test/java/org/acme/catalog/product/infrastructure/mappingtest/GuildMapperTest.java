package org.acme.catalog.product.infrastructure.mappingtest;

import org.acme.catalog.product.application.dto.GuildDTO;
import org.acme.catalog.product.application.dto.usecases.CreateGuildRequest;
import org.acme.catalog.product.application.dto.usecases.CreateUserRequest;
import org.acme.catalog.product.application.dto.usecases.UpdateGuildRequest;
import org.acme.catalog.product.infrastructure.mapper.GuildMapper;
import org.acme.catalog.product.infrastructure.mapper.UserMapper;
import org.acme.catalog.product.infrastructure.persistence.ChannelEntity;
import org.acme.catalog.product.infrastructure.persistence.GuildEntity;
import org.acme.catalog.product.infrastructure.persistence.RoleEntity;
import org.acme.catalog.product.infrastructure.persistence.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuildMapperTest {

    @Test
    void shouldMapEntityToDtoWithRelationIdentifiers() {
        UUID guildId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 2, 12, 0);
        GuildEntity guild = new GuildEntity("Guild", "Description", createdAt);
        RoleEntity role = new RoleEntity();
        ChannelEntity channel = new ChannelEntity();
        UserEntity member = new UserEntity();
        MapperTestSupport.setId(guild, guildId);
        MapperTestSupport.setId(role, roleId);
        MapperTestSupport.setId(channel, channelId);
        MapperTestSupport.setId(member, memberId);
        guild.getRoles().add(role);
        guild.getChannels().add(channel);
        guild.getMembers().add(member);

        GuildDTO dto = GuildMapper.toDto(guild);

        assertEquals(guildId, dto.id());
        assertEquals("Guild", dto.name());
        assertEquals("Description", dto.description());
        assertEquals(createdAt, dto.createdAt());
        assertEquals(Set.of(roleId), dto.roleIds());
        assertEquals(Set.of(channelId), dto.channelIds());
        assertEquals(Set.of(memberId), dto.memberIds());
        assertThrows(UnsupportedOperationException.class, () -> dto.memberIds().add(UUID.randomUUID()));
    }

    @Test
    void shouldCreateEntityWithServerGeneratedDate() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-03T09:45:00Z"), ZoneOffset.UTC);

        GuildEntity entity = GuildMapper.toEntity(new CreateGuildRequest("Guild", null), clock);

        assertNull(entity.getId());
        assertEquals("Guild", entity.getName());
        assertNull(entity.getDescription());
        assertEquals(LocalDateTime.of(2026, 7, 3, 9, 45), entity.getCreatedAt());
        assertEquals(Set.of(), entity.getRoles());
        assertEquals(Set.of(), entity.getChannels());
        assertEquals(Set.of(), entity.getMembers());
    }

    @Test
    void shouldGenerateSameUtcDateForClocksWithDifferentZones() {
        Instant instant = Instant.parse("2026-07-01T12:30:45Z");
        Clock parisClock = Clock.fixed(instant, ZoneId.of("Europe/Paris"));
        Clock utcClock = Clock.fixed(instant, ZoneOffset.UTC);

        GuildEntity fromParis = GuildMapper.toEntity(new CreateGuildRequest("Paris", null), parisClock);
        GuildEntity fromUtc = GuildMapper.toEntity(new CreateGuildRequest("UTC", null), utcClock);

        LocalDateTime expectedUtc = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        assertEquals(expectedUtc, fromParis.getCreatedAt());
        assertEquals(expectedUtc, fromUtc.getCreatedAt());
        assertEquals(fromParis.getCreatedAt(), fromUtc.getCreatedAt());
    }

    @Test
    void shouldGenerateIdenticalUserAndGuildDatesForSameInstantRegardlessOfClockZone() {
        Instant instant = Instant.parse("2026-10-25T01:30:00Z");
        Clock parisClock = Clock.fixed(instant, ZoneId.of("Europe/Paris"));
        Clock utcClock = Clock.fixed(instant, ZoneOffset.UTC);

        UserEntity user = UserMapper.toEntity(new CreateUserRequest("same-instant", "Same Instant"), parisClock);
        GuildEntity guild = GuildMapper.toEntity(new CreateGuildRequest("Same Instant", null), utcClock);

        assertEquals(user.getJoinedAt(), guild.getCreatedAt());
        assertEquals(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), guild.getCreatedAt());
    }

    @Test
    void shouldUseUtcForDefaultServerClock() {
        Instant before = Instant.now();

        GuildEntity entity = GuildMapper.toEntity(new CreateGuildRequest("Default Clock", null));

        Instant after = Instant.now();
        Instant generated = entity.getCreatedAt().toInstant(ZoneOffset.UTC);
        Duration tolerance = Duration.ofSeconds(1);
        assertFalse(generated.isBefore(before.minus(tolerance)));
        assertFalse(generated.isAfter(after.plus(tolerance)));
    }

    @Test
    void shouldUpdateEditableFieldsAndPreserveServerManagedFields() {
        UUID guildId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        GuildEntity entity = new GuildEntity("Before", "Old", createdAt);
        RoleEntity role = new RoleEntity();
        ChannelEntity channel = new ChannelEntity();
        UserEntity member = new UserEntity();
        MapperTestSupport.setId(entity, guildId);
        entity.getRoles().add(role);
        entity.getChannels().add(channel);
        entity.getMembers().add(member);

        GuildEntity updated = GuildMapper.updateEntity(new UpdateGuildRequest("After", "New"), entity);

        assertSame(entity, updated);
        assertEquals(guildId, updated.getId());
        assertEquals(createdAt, updated.getCreatedAt());
        assertEquals("After", updated.getName());
        assertEquals("New", updated.getDescription());
        assertEquals(Set.of(role), updated.getRoles());
        assertEquals(Set.of(channel), updated.getChannels());
        assertEquals(Set.of(member), updated.getMembers());
    }
}
