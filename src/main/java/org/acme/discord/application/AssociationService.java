package org.acme.discord.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import org.acme.discord.application.dto.PageDTO;
import org.acme.discord.application.dto.PageRequestDTO;
import org.acme.discord.application.dto.RoleDTO;
import org.acme.discord.application.dto.UserDTO;
import org.acme.discord.application.exception.GuildNotFoundException;
import org.acme.discord.application.exception.RoleMembershipConflictException;
import org.acme.discord.application.exception.RoleNotFoundException;
import org.acme.discord.application.exception.UserNotFoundException;
import org.acme.discord.infrastructure.mapper.RoleMapper;
import org.acme.discord.infrastructure.mapper.UserMapper;
import org.acme.discord.infrastructure.persistence.GuildEntity;
import org.acme.discord.infrastructure.persistence.GuildRepository;
import org.acme.discord.infrastructure.persistence.RoleEntity;
import org.acme.discord.infrastructure.persistence.RoleRepository;
import org.acme.discord.infrastructure.persistence.UserEntity;
import org.acme.discord.infrastructure.persistence.UserRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service managing user memberships and role assignments.
 */
@ApplicationScoped
public class AssociationService {

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
     * @param pagination validated pagination parameters
     * @return paginated member DTOs in stable order
     */
    @Transactional
    public PageDTO<UserDTO> listMembers(UUID guildId, PageRequestDTO pagination) {
        if (guildRepository.findByIdOptional(guildId).isEmpty()) {
            throw new GuildNotFoundException();
        }
        PanacheQuery<UserEntity> query = userRepository.findByGuildId(guildId);
        long totalItems = query.count();
        List<UserDTO> items = query.page(Page.of(pagination.page(), pagination.size())).list().stream()
                .map(UserMapper::toDto)
                .toList();
        return PageDTO.of(items, pagination, totalItems);
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
     * @param pagination validated pagination parameters
     * @return paginated role DTOs in stable order
     */
    @Transactional
    public PageDTO<RoleDTO> listRoles(UUID userId, PageRequestDTO pagination) {
        if (userRepository.findByIdOptional(userId).isEmpty()) {
            throw new UserNotFoundException();
        }
        PanacheQuery<RoleEntity> query = roleRepository.findByUserId(userId);
        long totalItems = query.count();
        List<RoleDTO> items = query.page(Page.of(pagination.page(), pagination.size())).list().stream()
                .map(RoleMapper::toDto)
                .toList();
        return PageDTO.of(items, pagination, totalItems);
    }

    private UserEntity findUserForUpdate(UUID userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(UserNotFoundException::new);
    }
}
