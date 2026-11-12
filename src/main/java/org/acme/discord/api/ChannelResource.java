package org.acme.discord.api;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.acme.discord.application.ChannelService;
import org.acme.discord.application.dto.ChannelDTO;
import org.acme.discord.application.dto.PageDTO;
import org.acme.discord.application.dto.usecases.CreateChannelRequest;
import org.acme.discord.application.dto.usecases.UpdateChannelRequest;

import java.net.URI;
import java.util.UUID;

/**
 * REST resource exposing Channel CRUD operations.
 */
@Path("/api/channels")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ChannelResource {

    private final ChannelService channelService;

    @Inject
    public ChannelResource(ChannelService channelService) {
        this.channelService = channelService;
    }

    @POST
    public Response create(
            @Valid @NotNull(message = "Request body is required") CreateChannelRequest request,
            @Context UriInfo uriInfo) {
        ChannelDTO channel = channelService.create(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(channel.id().toString()).build();
        return Response.created(location).entity(channel).build();
    }

    @GET
    @Path("/{id}")
    public ChannelDTO get(@PathParam("id") String id) {
        return channelService.get(parseChannelId(id));
    }

    @GET
    public PageDTO<ChannelDTO> list(
            @DefaultValue("0") @QueryParam("page") String page,
            @DefaultValue("20") @QueryParam("size") String size) {
        return channelService.list(parseInteger(page, "page"), parseInteger(size, "size"));
    }

    @PUT
    @Path("/{id}")
    public ChannelDTO update(
            @PathParam("id") String id,
            @Valid @NotNull(message = "Request body is required") UpdateChannelRequest request) {
        return channelService.update(parseChannelId(id), request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        channelService.delete(parseChannelId(id));
        return Response.noContent().build();
    }

    private static UUID parseChannelId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid channel id");
        }
    }

    private static int parseInteger(String value, String parameterName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(parameterName + " must be an integer");
        }
    }
}
