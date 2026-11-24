package org.acme.discord.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.discord.application.dto.RoleDTO;
import org.acme.discord.application.dto.UserDTO;
import org.acme.discord.application.exception.GuildNotFoundException;
import org.acme.discord.application.exception.RoleMembershipConflictException;
import org.acme.discord.application.exception.RoleNotFoundException;
import org.acme.discord.infrastructure.mapper.RoleMapper;
import org.acme.discord.infrastructure.mapper.UserMapper;
import org.acme.discord.infrastructure.persistence.GuildEntity;
import org.acme.discord.infrastructure.persistence.GuildRepository;
import org.acme.discord.infrastructure.persistence.RoleEntity;
import org.acme.discord.infrastructure.persistence.RoleRepository;
import org.acme.discord.infrastructure.persistence.UserEntity;
import org.acme.discord.infrastructure.persistence.UserRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service managing user memberships and role assignments.
 */
@ApplicationScoped
public class AssociationService {

    private static final String USER_NOT_FOUND_MESSAGE = "User not found";

    private final UserRepository userRepository;
    private final GuildRepository guildRepository;
    private final RoleRepository roleRepository;

    public AssociationService(
            UserRepository userRepository,
            GuildRepository guildRepository,
            RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.guildRepository = guildRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Adds a user to a guild. Repeated additions have no effect.
     *
     * @param guildId guild identifier
     * @param userId user identifier
     */
    @Transactional
    public void addMember(UUID guildId, UUID userId) {
        UserEntity user = findUserForUpdate(userId);
        GuildEntity guild = guildRepository.findByIdForUpdate(guildId)
                .orElseThrow(GuildNotFoundException::new);

        if (user.getGuilds().add(guild)) {
            guild.getMembers().add(user);
            userRepository.flush();
        }
    }

    /**
     * Removes a user from a guild and removes only roles belonging to that guild.
     * Repeated removals have no effect.
     *
     * @param guildId guild identifier
     * @param userId user identifier
     */
    @Transactional
    public void removeMember(UUID guildId, UUID userId) {
        UserEntity user = findUserForUpdate(userId);
        GuildEntity guild = guildRepository.findByIdForUpdate(guildId)
                .orElseThrow(GuildNotFoundException::new);

        Set<RoleEntity> removedRoles = user.getRoles().stream()
                .filter(role -> guildId.equals(role.getGuild().getId()))
                .collect(Collectors.toSet());
        removedRoles.forEach(role -> role.getUsers().remove(user));
        user.getRoles().removeAll(removedRoles);

        user.getGuilds().remove(guild);
        guild.getMembers().remove(user);
        userRepository.flush();
    }

    /**
     * Lists a guild's members without exposing persistence entities.
     *
     * @param guildId guild identifier
     * @return member DTOs in stable order
     */
    @Transactional
    public List<UserDTO> listMembers(UUID guildId) {
        if (guildRepository.findByIdOptional(guildId).isEmpty()) {
            throw new GuildNotFoundException();
        }
        return userRepository.listByGuildId(guildId).stream()
                .map(UserMapper::toDto)
                .toList();
    }

    /**
     * Assigns a role to a user who belongs to the role's guild.
     * Repeated assignments have no effect.
     *
     * @param userId user identifier
     * @param roleId role identifier
     */
    @Transactional
    public void assignRole(UUID userId, UUID roleId) {
        UserEntity user = findUserForUpdate(userId);
        RoleEntity role = roleRepository.findByIdForUpdate(roleId)
                .orElseThrow(RoleNotFoundException::new);

        UUID guildId = role.getGuild().getId();
        boolean member = user.getGuilds().stream()
                .anyMatch(guild -> guildId.equals(guild.getId()));
        if (!member) {
            throw new RoleMembershipConflictException();
        }

        if (user.getRoles().add(role)) {
            role.getUsers().add(user);
            userRepository.flush();
        }
    }

    /**
     * Removes a role from a user. Repeated removals have no effect.
     *
     * @param userId user identifier
     * @param roleId role identifier
     */
    @Transactional
    public void removeRole(UUID userId, UUID roleId) {
        UserEntity user = findUserForUpdate(userId);
        RoleEntity role = roleRepository.findByIdForUpdate(roleId)
                .orElseThrow(RoleNotFoundException::new);

        user.getRoles().remove(role);
        role.getUsers().remove(user);
        userRepository.flush();
    }

    /**
     * Lists roles assigned to a user without exposing persistence entities.
     *
     * @param userId user identifier
     * @return role DTOs in stable order
     */
    @Transactional
    public List<RoleDTO> listRoles(UUID userId) {
        if (userRepository.findByIdOptional(userId).isEmpty()) {
            throw userNotFound();
        }
        return roleRepository.listByUserId(userId).stream()
                .map(RoleMapper::toDto)
                .toList();
    }

    private UserEntity findUserForUpdate(UUID userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(AssociationService::userNotFound);
    }

    private static NoSuchElementException userNotFound() {
        return new NoSuchElementException(USER_NOT_FOUND_MESSAGE);
    }
}
