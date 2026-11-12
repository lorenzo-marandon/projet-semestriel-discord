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
import org.acme.discord.application.GuildService;
import org.acme.discord.application.dto.GuildDTO;
import org.acme.discord.application.dto.PageDTO;
import org.acme.discord.application.dto.usecases.CreateGuildRequest;
import org.acme.discord.application.dto.usecases.UpdateGuildRequest;

import java.net.URI;
import java.util.UUID;

/**
 * REST resource exposing Guild CRUD operations.
 */
@Path("/api/guilds")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GuildResource {

    private final GuildService guildService;

    @Inject
    public GuildResource(GuildService guildService) {
        this.guildService = guildService;
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

    private static UUID parseGuildId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid guild id");
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
