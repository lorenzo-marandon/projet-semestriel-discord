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
import org.acme.discord.application.dto.PageRequestDTO;
import org.acme.discord.application.dto.UserDTO;
import org.acme.discord.application.dto.usecases.CreateGuildRequest;
import org.acme.discord.application.dto.usecases.UpdateGuildRequest;
import org.acme.discord.application.exception.InvalidRequestException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.UUID;

/**
 * REST resource exposing Guild CRUD operations.
 */
@Path("/api/guilds")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Guilds", description = "Guild lifecycle and membership operations")
public class GuildResource {

    private final GuildService guildService;
    private final AssociationService associationService;

    @Inject
    public GuildResource(GuildService guildService, AssociationService associationService) {
        this.guildService = guildService;
        this.associationService = associationService;
    }

    @POST
    @Operation(summary = "Create a guild", description = "Creates a guild. Its identifier, creation date, and associations are server-controlled.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Guild created",
                    headers = @Header(name = "Location", description = "URI of the created guild",
                            schema = @Schema(type = SchemaType.STRING, format = "uri")),
                    content = @Content(schema = @Schema(implementation = GuildDTO.class))),
            @APIResponse(responseCode = "400", description = "Missing, malformed, or invalid request body",
                    content = @Content(schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "415", description = "Content-Type is not application/json"),
            @APIResponse(responseCode = "500", description = "Unexpected internal error",
                    content = @Content(schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public Response create(
            @RequestBody(required = true, description = "Guild creation payload")
            @Valid @NotNull(message = "Request body is required") CreateGuildRequest request,
            @Context UriInfo uriInfo) {
        GuildDTO guild = guildService.create(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(guild.id().toString()).build();
        return Response.created(location).entity(guild).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a guild", description = "Returns one guild by its UUID.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Guild found"),
            @APIResponse(responseCode = "400", description = "Invalid UUID", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "GUILD_NOT_FOUND", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public GuildDTO get(
            @Parameter(description = "Guild identifier", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("id") String id) {
        return guildService.get(parseGuildId(id));
    }

    @GET
    @Operation(summary = "List guilds", description = "Returns guilds ordered by creation date, then UUID. Empty and out-of-range pages have no items.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated guilds"),
            @APIResponse(responseCode = "400", description = "INVALID_REQUEST: invalid pagination or filter", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public PageDTO<GuildDTO> list(
            @Parameter(description = "Zero-based page index", schema = @Schema(type = SchemaType.INTEGER,
                    defaultValue = "0", minimum = "0"))
            @DefaultValue(QueryParameters.DEFAULT_PAGE) @QueryParam("page") String page,
            @Parameter(description = "Page size from 1 to 100", schema = @Schema(type = SchemaType.INTEGER,
                    defaultValue = "20", minimum = "1", maximum = "100"))
            @DefaultValue(QueryParameters.DEFAULT_SIZE) @QueryParam("size") String size,
            @Parameter(description = "Optional non-empty, trimmed, case-insensitive partial guild name. Percent and underscore are matched literally.",
                    example = "demo")
            @QueryParam("name") String name,
            @Context UriInfo uriInfo) {
        PageRequestDTO pagination = QueryParameters.pagination(page, size);
        return guildService.list(pagination, QueryParameters.optionalText(
                name, "name", uriInfo.getQueryParameters().containsKey("name")));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a guild", description = "Fully replaces the editable name and description. Server-controlled fields are unchanged.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Guild updated"),
            @APIResponse(responseCode = "400", description = "Invalid UUID or request body", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "GUILD_NOT_FOUND", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "415", description = "Content-Type is not application/json"),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public GuildDTO update(
            @Parameter(description = "Guild identifier", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("id") String id,
            @RequestBody(required = true, description = "Complete set of editable guild fields")
            @Valid @NotNull(message = "Request body is required") UpdateGuildRequest request) {
        return guildService.update(parseGuildId(id), request);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a guild", description = "Deletes an empty guild. Guilds with members, roles, or channels cannot be deleted.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Guild deleted; no response body"),
            @APIResponse(responseCode = "400", description = "Invalid UUID", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "GUILD_NOT_FOUND", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "409", description = "GUILD_NOT_EMPTY", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public Response delete(
            @Parameter(description = "Guild identifier", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("id") String id) {
        guildService.delete(parseGuildId(id));
        return Response.noContent().build();
    }

    @PUT
    @Path("/{guildId}/members/{userId}")
    @Operation(summary = "Add a guild member", description = "Idempotently adds a user to a guild.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Membership added or already present; no response body"),
            @APIResponse(responseCode = "400", description = "Invalid UUID", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "GUILD_NOT_FOUND or USER_NOT_FOUND", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public Response addMember(
            @Parameter(description = "Guild receiving the member", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("guildId") String guildId,
            @Parameter(description = "User to add", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("userId") String userId) {
        associationService.addMember(parseGuildId(guildId), parseUserId(userId));
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{guildId}/members/{userId}")
    @Operation(summary = "Remove a guild member", description = "Idempotently removes a member. Role assignments owned by this guild are removed atomically; assignments in other guilds are preserved.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Membership removed or already absent; no response body"),
            @APIResponse(responseCode = "400", description = "Invalid UUID", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "GUILD_NOT_FOUND or USER_NOT_FOUND", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public Response removeMember(
            @Parameter(description = "Guild losing the member", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("guildId") String guildId,
            @Parameter(description = "User to remove", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("userId") String userId) {
        associationService.removeMember(parseGuildId(guildId), parseUserId(userId));
        return Response.noContent().build();
    }

    @GET
    @Path("/{guildId}/members")
    @Operation(summary = "List guild members", description = "Returns members ordered by join date, then UUID. Empty and out-of-range pages have no items.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated members"),
            @APIResponse(responseCode = "400", description = "Invalid UUID or pagination", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "GUILD_NOT_FOUND", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public PageDTO<UserDTO> listMembers(
            @Parameter(description = "Guild whose members are listed", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("guildId") String guildId,
            @Parameter(description = "Zero-based page index", schema = @Schema(type = SchemaType.INTEGER,
                    defaultValue = "0", minimum = "0"))
            @DefaultValue(QueryParameters.DEFAULT_PAGE) @QueryParam("page") String page,
            @Parameter(description = "Page size from 1 to 100", schema = @Schema(type = SchemaType.INTEGER,
                    defaultValue = "20", minimum = "1", maximum = "100"))
            @DefaultValue(QueryParameters.DEFAULT_SIZE) @QueryParam("size") String size) {
        return associationService.listMembers(
                parseGuildId(guildId), QueryParameters.pagination(page, size));
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

}
