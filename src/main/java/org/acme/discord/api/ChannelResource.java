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
import org.acme.discord.application.dto.PageRequestDTO;
import org.acme.discord.application.dto.usecases.CreateChannelRequest;
import org.acme.discord.application.dto.usecases.UpdateChannelRequest;
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
 * REST resource exposing Channel CRUD operations.
 */
@Path("/api/channels")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Channels", description = "Guild channel lifecycle operations")
public class ChannelResource {

    private final ChannelService channelService;

    @Inject
    public ChannelResource(ChannelService channelService) {
        this.channelService = channelService;
    }

    @POST
    @Operation(summary = "Create a channel", description = "Creates a TEXT or VOICE channel owned by an existing guild. The identifier and owning guild are not editable later.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Channel created",
                    headers = @Header(name = "Location", description = "URI of the created channel",
                            schema = @Schema(type = SchemaType.STRING, format = "uri")),
                    content = @Content(schema = @Schema(implementation = ChannelDTO.class))),
            @APIResponse(responseCode = "400", description = "Missing, malformed, or invalid request body", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "GUILD_NOT_FOUND", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "415", description = "Content-Type is not application/json"),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public Response create(
            @RequestBody(required = true, description = "Channel creation payload")
            @Valid @NotNull(message = "Request body is required") CreateChannelRequest request,
            @Context UriInfo uriInfo) {
        ChannelDTO channel = channelService.create(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(channel.id().toString()).build();
        return Response.created(location).entity(channel).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a channel", description = "Returns one channel by its UUID.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Channel found"),
            @APIResponse(responseCode = "400", description = "Invalid UUID", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "CHANNEL_NOT_FOUND", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public ChannelDTO get(
            @Parameter(description = "Channel identifier", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("id") String id) {
        return channelService.get(parseChannelId(id));
    }

    @GET
    @Operation(summary = "List channels", description = "Returns channels ordered by position, then UUID. Guild and type filters are combined with AND. A valid filter with no match returns an empty page.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated channels"),
            @APIResponse(responseCode = "400", description = "INVALID_REQUEST: invalid pagination, guild UUID, or channel type", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public PageDTO<ChannelDTO> list(
            @Parameter(description = "Zero-based page index", schema = @Schema(type = SchemaType.INTEGER,
                    defaultValue = "0", minimum = "0"))
            @DefaultValue(QueryParameters.DEFAULT_PAGE) @QueryParam("page") String page,
            @Parameter(description = "Page size from 1 to 100", schema = @Schema(type = SchemaType.INTEGER,
                    defaultValue = "20", minimum = "1", maximum = "100"))
            @DefaultValue(QueryParameters.DEFAULT_SIZE) @QueryParam("size") String size,
            @Parameter(description = "Optional exact guild UUID. A supplied empty value is invalid.",
                    example = "00000000-0000-4000-8000-000000000201",
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @QueryParam("guildId") String guildId,
            @Parameter(description = "Optional exact channel type after trimming. A supplied empty or unknown value is invalid.",
                    example = "TEXT", schema = @Schema(type = SchemaType.STRING, enumeration = {"TEXT", "VOICE"}))
            @QueryParam("type") String type,
            @Context UriInfo uriInfo) {
        PageRequestDTO pagination = QueryParameters.pagination(page, size);
        return channelService.list(
                pagination,
                QueryParameters.optionalUuid(
                        guildId, "guildId", uriInfo.getQueryParameters().containsKey("guildId")),
                QueryParameters.optionalText(
                        type, "type", uriInfo.getQueryParameters().containsKey("type")));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a channel", description = "Fully replaces the editable name, type, and position. The owning guild is unchanged.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Channel updated"),
            @APIResponse(responseCode = "400", description = "Invalid UUID or request body", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "CHANNEL_NOT_FOUND", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "415", description = "Content-Type is not application/json"),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public ChannelDTO update(
            @Parameter(description = "Channel identifier", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("id") String id,
            @RequestBody(required = true, description = "Complete set of editable channel fields")
            @Valid @NotNull(message = "Request body is required") UpdateChannelRequest request) {
        return channelService.update(parseChannelId(id), request);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a channel", description = "Deletes a channel by UUID.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Channel deleted; no response body"),
            @APIResponse(responseCode = "400", description = "Invalid UUID", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "CHANNEL_NOT_FOUND", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public Response delete(
            @Parameter(description = "Channel identifier", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("id") String id) {
        channelService.delete(parseChannelId(id));
        return Response.noContent().build();
    }

    private static UUID parseChannelId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Invalid channel id");
        }
    }

}
