package org.acme.catalog.product.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA entity representing a guild in the database.
 */
@Entity
@Table(name = "guilds")
public class GuildEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "guild", fetch = FetchType.LAZY)
    private Set<RoleEntity> roles = new HashSet<>();

    @OneToMany(mappedBy = "guild", fetch = FetchType.LAZY)
    private Set<ChannelEntity> channels = new HashSet<>();

    @ManyToMany(mappedBy = "guilds", fetch = FetchType.LAZY)
    private Set<UserEntity> members = new HashSet<>();

    public GuildEntity() {
    }

    public GuildEntity(String name, String description, LocalDateTime createdAt) {
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleEntity> roles) {
        this.roles = roles;
    }

    public Set<ChannelEntity> getChannels() {
        return channels;
    }

    public void setChannels(Set<ChannelEntity> channels) {
        this.channels = channels;
    }

    public Set<UserEntity> getMembers() {
        return members;
    }

    public void setMembers(Set<UserEntity> members) {
        this.members = members;
    }
}
