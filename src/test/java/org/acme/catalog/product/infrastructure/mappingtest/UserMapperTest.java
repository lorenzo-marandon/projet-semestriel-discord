package org.acme.catalog.product.infrastructure.mappingtest;

import org.acme.catalog.product.application.dto.UserDTO;
import org.acme.catalog.product.application.dto.usecases.CreateUserRequest;
import org.acme.catalog.product.application.dto.usecases.UpdateUserRequest;
import org.acme.catalog.product.infrastructure.mapper.UserMapper;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserMapperTest {

    @Test
    void shouldMapEntityToDtoWithRelationIdentifiers() {
        UUID userId = UUID.randomUUID();
        UUID guildId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        LocalDateTime joinedAt = LocalDateTime.of(2026, 5, 1, 10, 30);
        UserEntity user = new UserEntity("alice", "Alice", joinedAt);
        GuildEntity guild = new GuildEntity();
        RoleEntity role = new RoleEntity();
        MapperTestSupport.setId(user, userId);
        MapperTestSupport.setId(guild, guildId);
        MapperTestSupport.setId(role, roleId);
        user.getGuilds().add(guild);
        user.getRoles().add(role);

        UserDTO dto = UserMapper.toDto(user);

        assertEquals(userId, dto.id());
        assertEquals("alice", dto.username());
        assertEquals("Alice", dto.displayName());
        assertEquals(joinedAt, dto.joinedAt());
        assertEquals(java.util.Set.of(guildId), dto.guildIds());
        assertEquals(java.util.Set.of(roleId), dto.roleIds());
        assertThrows(UnsupportedOperationException.class, () -> dto.guildIds().add(UUID.randomUUID()));
    }

    @Test
    void shouldCreateEntityWithServerGeneratedDate() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-01T08:15:30Z"), ZoneOffset.UTC);

        UserEntity entity = UserMapper.toEntity(new CreateUserRequest("bob", "Bob"), clock);

        assertNull(entity.getId());
        assertEquals("bob", entity.getUsername());
        assertEquals("Bob", entity.getDisplayName());
        assertEquals(LocalDateTime.of(2026, 6, 1, 8, 15, 30), entity.getJoinedAt());
        assertEquals(java.util.Set.of(), entity.getGuilds());
        assertEquals(java.util.Set.of(), entity.getRoles());
    }

    @Test
    void shouldGenerateSameUtcDateForClocksWithDifferentZones() {
        Instant instant = Instant.parse("2026-07-01T12:30:45Z");
        Clock parisClock = Clock.fixed(instant, ZoneId.of("Europe/Paris"));
        Clock utcClock = Clock.fixed(instant, ZoneOffset.UTC);

        UserEntity fromParis = UserMapper.toEntity(new CreateUserRequest("paris", "Paris"), parisClock);
        UserEntity fromUtc = UserMapper.toEntity(new CreateUserRequest("utc", "UTC"), utcClock);

        LocalDateTime expectedUtc = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        assertEquals(expectedUtc, fromParis.getJoinedAt());
        assertEquals(expectedUtc, fromUtc.getJoinedAt());
        assertEquals(fromParis.getJoinedAt(), fromUtc.getJoinedAt());
    }

    @Test
    void shouldUseUtcForDefaultServerClock() {
        Instant before = Instant.now();

        UserEntity entity = UserMapper.toEntity(new CreateUserRequest("default-clock", "Default Clock"));

        Instant after = Instant.now();
        Instant generated = entity.getJoinedAt().toInstant(ZoneOffset.UTC);
        Duration tolerance = Duration.ofSeconds(1);
        assertFalse(generated.isBefore(before.minus(tolerance)));
        assertFalse(generated.isAfter(after.plus(tolerance)));
    }

    @Test
    void shouldUpdateEditableFieldsAndPreserveServerManagedFields() {
        UUID userId = UUID.randomUUID();
        LocalDateTime joinedAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        UserEntity entity = new UserEntity("before", "Before", joinedAt);
        GuildEntity guild = new GuildEntity();
        RoleEntity role = new RoleEntity();
        MapperTestSupport.setId(entity, userId);
        entity.getGuilds().add(guild);
        entity.getRoles().add(role);

        UserEntity updated = UserMapper.updateEntity(new UpdateUserRequest("after", "After"), entity);

        assertSame(entity, updated);
        assertEquals(userId, updated.getId());
        assertEquals(joinedAt, updated.getJoinedAt());
        assertEquals("after", updated.getUsername());
        assertEquals("After", updated.getDisplayName());
        assertEquals(java.util.Set.of(guild), updated.getGuilds());
        assertEquals(java.util.Set.of(role), updated.getRoles());
    }
}
