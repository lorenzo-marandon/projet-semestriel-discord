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
import org.acme.discord.application.RoleService;
import org.acme.discord.application.dto.PageDTO;
import org.acme.discord.application.dto.PageRequestDTO;
import org.acme.discord.application.dto.RoleDTO;
import org.acme.discord.application.dto.usecases.CreateRoleRequest;
import org.acme.discord.application.dto.usecases.UpdateRoleRequest;
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
 * REST resource exposing Role CRUD operations.
 */
@Path("/api/roles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Roles", description = "Guild role lifecycle operations")
public class RoleResource {

    private final RoleService roleService;

    @Inject
    public RoleResource(RoleService roleService) {
        this.roleService = roleService;
    }

    @POST
    @Operation(summary = "Create a role", description = "Creates a role owned by an existing guild. The identifier and owning guild are not editable later.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Role created",
                    headers = @Header(name = "Location", description = "URI of the created role",
                            schema = @Schema(type = SchemaType.STRING, format = "uri")),
                    content = @Content(schema = @Schema(implementation = RoleDTO.class))),
            @APIResponse(responseCode = "400", description = "Missing, malformed, or invalid request body", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "GUILD_NOT_FOUND", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "415", description = "Content-Type is not application/json"),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public Response create(
            @RequestBody(required = true, description = "Role creation payload")
            @Valid @NotNull(message = "Request body is required") CreateRoleRequest request,
            @Context UriInfo uriInfo) {
        RoleDTO role = roleService.create(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(role.id().toString()).build();
        return Response.created(location).entity(role).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a role", description = "Returns one role by its UUID.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Role found"),
            @APIResponse(responseCode = "400", description = "Invalid UUID", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "ROLE_NOT_FOUND", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public RoleDTO get(
            @Parameter(description = "Role identifier", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("id") String id) {
        return roleService.get(parseRoleId(id));
    }

    @GET
    @Operation(summary = "List roles", description = "Returns roles ordered by position, then UUID. A valid guild filter with no match returns an empty page.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated roles"),
            @APIResponse(responseCode = "400", description = "INVALID_REQUEST: invalid pagination or guild UUID", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public PageDTO<RoleDTO> list(
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
            @Context UriInfo uriInfo) {
        PageRequestDTO pagination = QueryParameters.pagination(page, size);
        return roleService.list(pagination, QueryParameters.optionalUuid(
                guildId, "guildId", uriInfo.getQueryParameters().containsKey("guildId")));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a role", description = "Fully replaces the editable name, color, and position. The owning guild is unchanged.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Role updated"),
            @APIResponse(responseCode = "400", description = "Invalid UUID or request body", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "ROLE_NOT_FOUND", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "415", description = "Content-Type is not application/json"),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public RoleDTO update(
            @Parameter(description = "Role identifier", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("id") String id,
            @RequestBody(required = true, description = "Complete set of editable role fields")
            @Valid @NotNull(message = "Request body is required") UpdateRoleRequest request) {
        return roleService.update(parseRoleId(id), request);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a role", description = "Deletes an unassigned role. Roles currently assigned to users cannot be deleted.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Role deleted; no response body"),
            @APIResponse(responseCode = "400", description = "Invalid UUID", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "ROLE_NOT_FOUND", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "409", description = "ROLE_IN_USE", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(schema = @Schema(
                    implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public Response delete(
            @Parameter(description = "Role identifier", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("id") String id) {
        roleService.delete(parseRoleId(id));
        return Response.noContent().build();
    }

    private static UUID parseRoleId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Invalid role id");
        }
    }

}
