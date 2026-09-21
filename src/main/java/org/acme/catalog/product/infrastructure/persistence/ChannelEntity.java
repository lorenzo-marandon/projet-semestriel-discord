package org.acme.catalog.product.infrastructure.persistence;

import org.acme.catalog.product.domain.ChannelType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

/**
 * JPA entity representing a channel in the database.
 */
@Entity
@Table(name = "channels")
public class ChannelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Channel name is required")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Channel type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType type;

    @NotNull(message = "Channel position is required")
    @PositiveOrZero(message = "Channel position cannot be negative")
    @Column(nullable = false)
    private Integer position;

    @NotNull(message = "Channel guild is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guild_id", nullable = false)
    private GuildEntity guild;

    public ChannelEntity() {
    }

    public ChannelEntity(String name, ChannelType type, Integer position) {
        this.name = name;
        this.type = type;
        this.position = position;
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

    public ChannelType getType() {
        return type;
    }

    public void setType(ChannelType type) {
        this.type = type;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public GuildEntity getGuild() {
        return guild;
    }

    public void setGuild(GuildEntity guild) {
        this.guild = guild;
    }
}
