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
import org.acme.discord.application.AssociationService;
import org.acme.discord.application.GuildService;
import org.acme.discord.application.dto.GuildDTO;
import org.acme.discord.application.dto.PageDTO;
import org.acme.discord.application.dto.UserDTO;
import org.acme.discord.application.dto.usecases.CreateGuildRequest;
import org.acme.discord.application.dto.usecases.UpdateGuildRequest;
import org.acme.discord.application.exception.InvalidRequestException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST resource exposing Guild CRUD operations.
 */
@Path("/api/guilds")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GuildResource {

    private final GuildService guildService;
    private final AssociationService associationService;

    @Inject
    public GuildResource(GuildService guildService, AssociationService associationService) {
        this.guildService = guildService;
        this.associationService = associationService;
    }

    @POST
    public Response create(
            @Valid @NotNull(message = "Request body is required") CreateGuildRequest request,
            @Context UriInfo uriInfo) {
        GuildDTO guild = guildService.create(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(guild.id().toString()).build();
        return Response.created(location).entity(guild).build();
    }

    @GET
    @Path("/{id}")
    public GuildDTO get(@PathParam("id") String id) {
        return guildService.get(parseGuildId(id));
    }

    @GET
    public PageDTO<GuildDTO> list(
            @DefaultValue("0") @QueryParam("page") String page,
            @DefaultValue("20") @QueryParam("size") String size) {
        return guildService.list(parseInteger(page, "page"), parseInteger(size, "size"));
    }

    @PUT
    @Path("/{id}")
    public GuildDTO update(
            @PathParam("id") String id,
            @Valid @NotNull(message = "Request body is required") UpdateGuildRequest request) {
        return guildService.update(parseGuildId(id), request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        guildService.delete(parseGuildId(id));
        return Response.noContent().build();
    }

    @PUT
    @Path("/{guildId}/members/{userId}")
    public Response addMember(
            @PathParam("guildId") String guildId,
            @PathParam("userId") String userId) {
        associationService.addMember(parseGuildId(guildId), parseUserId(userId));
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{guildId}/members/{userId}")
    public Response removeMember(
            @PathParam("guildId") String guildId,
            @PathParam("userId") String userId) {
        associationService.removeMember(parseGuildId(guildId), parseUserId(userId));
        return Response.noContent().build();
    }

    @GET
    @Path("/{guildId}/members")
    public List<UserDTO> listMembers(@PathParam("guildId") String guildId) {
        return associationService.listMembers(parseGuildId(guildId));
    }

    private static UUID parseGuildId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Invalid guild id");
        }
    }

    private static UUID parseUserId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Invalid user id");
        }
    }

    private static int parseInteger(String value, String parameterName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new InvalidRequestException(parameterName + " must be an integer");
        }
    }
}
