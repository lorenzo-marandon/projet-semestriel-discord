package org.acme.discord.application;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.discord.application.dto.ChannelDTO;
import org.acme.discord.application.dto.PageDTO;
import org.acme.discord.application.dto.PageRequestDTO;
import org.acme.discord.application.dto.usecases.CreateChannelRequest;
import org.acme.discord.application.dto.usecases.UpdateChannelRequest;
import org.acme.discord.application.exception.ChannelNotFoundException;
import org.acme.discord.application.exception.InvalidRequestException;
import org.acme.discord.application.exception.GuildNotFoundException;
import org.acme.discord.infrastructure.mapper.ChannelMapper;
import org.acme.discord.infrastructure.persistence.ChannelEntity;
import org.acme.discord.infrastructure.persistence.ChannelRepository;
import org.acme.discord.infrastructure.persistence.GuildEntity;
import org.acme.discord.infrastructure.persistence.GuildRepository;
import org.acme.discord.domain.ChannelType;

import java.util.List;
import java.util.UUID;

/**
 * Application service implementing Channel CRUD use cases.
 */
@ApplicationScoped
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final GuildRepository guildRepository;

    public ChannelService(ChannelRepository channelRepository, GuildRepository guildRepository) {
        this.channelRepository = channelRepository;
        this.guildRepository = guildRepository;
    }

    /**
     * Creates and persists a channel in an existing guild.
     *
     * @param request validated creation request
     * @return created channel
     */
    @Transactional
    public ChannelDTO create(CreateChannelRequest request) {
        GuildEntity guild = guildRepository.findByIdForRead(request.guildId())
                .orElseThrow(GuildNotFoundException::new);
        ChannelEntity entity = ChannelMapper.toEntity(request, guild);
        channelRepository.persistAndFlush(entity);
        return ChannelMapper.toDto(entity);
    }

    /**
     * Retrieves a channel by identifier.
     *
     * @param id channel identifier
     * @return matching channel
     */
    @Transactional
    public ChannelDTO get(UUID id) {
        return ChannelMapper.toDto(findEntity(id));
    }

    /**
     * Lists channels using stable database ordering and pagination.
     *
     * @param pagination validated pagination parameters
     * @param guildId optional guild identifier filter
     * @param type optional exact channel type filter
     * @return paginated channels
     */
    @Transactional
    public PageDTO<ChannelDTO> list(PageRequestDTO pagination, UUID guildId, String type) {
        PanacheQuery<ChannelEntity> query = channelRepository.findForList(guildId, parseType(type));
        long totalItems = query.count();
        List<ChannelDTO> items = query.page(Page.of(pagination.page(), pagination.size())).list().stream()
                .map(ChannelMapper::toDto)
                .toList();
        return PageDTO.of(items, pagination, totalItems);
    }

    /**
     * Fully updates a channel's editable fields while preserving its guild.
     *
     * @param id channel identifier
     * @param request validated update request
     * @return updated channel
     */
    @Transactional
    public ChannelDTO update(UUID id, UpdateChannelRequest request) {
        ChannelEntity entity = findEntity(id);
        ChannelMapper.updateEntity(request, entity);
        channelRepository.flush();
        return ChannelMapper.toDto(entity);
    }

    /**
     * Deletes a channel while protecting it from concurrent changes.
     *
     * @param id channel identifier
     */
    @Transactional
    public void delete(UUID id) {
        ChannelEntity entity = channelRepository.findByIdForUpdate(id)
                .orElseThrow(ChannelNotFoundException::new);
        channelRepository.delete(entity);
        channelRepository.flush();
    }

    private ChannelEntity findEntity(UUID id) {
        return channelRepository.findByIdOptional(id)
                .orElseThrow(ChannelNotFoundException::new);
    }

    private static ChannelType parseType(String type) {
        if (type == null) {
            return null;
        }
        try {
            return ChannelType.valueOf(type);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Invalid channel type");
        }
    }
}
