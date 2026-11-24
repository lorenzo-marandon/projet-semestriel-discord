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
import org.acme.discord.application.UserService;
import org.acme.discord.application.dto.PageDTO;
import org.acme.discord.application.dto.PageRequestDTO;
import org.acme.discord.application.dto.RoleDTO;
import org.acme.discord.application.dto.UserDTO;
import org.acme.discord.application.dto.usecases.CreateUserRequest;
import org.acme.discord.application.dto.usecases.UpdateUserRequest;
import org.acme.discord.application.exception.InvalidRequestException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.UUID;

/**
 * REST resource exposing User CRUD operations.
 */
@Path("/api/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "User lifecycle and role assignment operations")
public class UserResource {

    private final UserService userService;
    private final AssociationService associationService;

    @Inject
    public UserResource(UserService userService, AssociationService associationService) {
        this.userService = userService;
        this.associationService = associationService;
    }

    @POST
    @Operation(summary = "Create a user",
            description = "Creates a user with a unique username. The identifier, join date, and associations are server-controlled.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "User created",
                    headers = @Header(name = "Location", description = "URI of the created user",
                            schema = @Schema(type = SchemaType.STRING, format = "uri")),
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @APIResponse(responseCode = "400", description = "Missing, malformed, or invalid request body",
                    content = @Content(schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class),
                            examples = @ExampleObject(name = "validation", value = OpenApiExamples.VALIDATION_ERROR))),
            @APIResponse(responseCode = "409", description = "USERNAME_ALREADY_EXISTS",
                    content = @Content(schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class),
                            examples = @ExampleObject(name = "usernameConflict", value = OpenApiExamples.CONFLICT))),
            @APIResponse(responseCode = "415", description = "Content-Type is not application/json"),
            @APIResponse(responseCode = "500", description = "Unexpected internal error",
                    content = @Content(schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class),
                            examples = @ExampleObject(name = "internalError", value = OpenApiExamples.INTERNAL_ERROR)))
    })
    public Response create(
            @RequestBody(required = true, description = "User creation payload")
            @Valid @NotNull(message = "Request body is required") CreateUserRequest request,
            @Context UriInfo uriInfo) {
        UserDTO user = userService.create(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(user.id().toString()).build();
        return Response.created(location).entity(user).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a user", description = "Returns one user by its UUID.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "User found"),
            @APIResponse(responseCode = "400", description = "Invalid UUID", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class),
                    examples = @ExampleObject(name = "invalidUuid", value = OpenApiExamples.INVALID_REQUEST))),
            @APIResponse(responseCode = "404", description = "USER_NOT_FOUND", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class),
                    examples = @ExampleObject(name = "notFound", value = OpenApiExamples.NOT_FOUND))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public UserDTO get(
            @Parameter(description = "User identifier", required = true, example = "00000000-0000-4000-8000-000000000101",
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("id") String id) {
        return userService.get(parseUserId(id));
    }

    @GET
    @Operation(summary = "List users", description = "Returns a deterministically ordered page of users (join date, then UUID). "
            + "Empty and out-of-range pages return an empty items array.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated users"),
            @APIResponse(responseCode = "400", description = "INVALID_REQUEST: invalid pagination or filter", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public PageDTO<UserDTO> list(
            @Parameter(description = "Zero-based page index", example = "0",
                    schema = @Schema(type = SchemaType.INTEGER, defaultValue = "0", minimum = "0"))
            @DefaultValue(QueryParameters.DEFAULT_PAGE) @QueryParam("page") String page,
            @Parameter(description = "Page size from 1 to 100", example = "20",
                    schema = @Schema(type = SchemaType.INTEGER, defaultValue = "20", minimum = "1", maximum = "100"))
            @DefaultValue(QueryParameters.DEFAULT_SIZE) @QueryParam("size") String size,
            @Parameter(description = "Optional non-empty, trimmed, case-insensitive partial username. Percent and underscore are matched literally.",
                    example = "demo")
            @QueryParam("username") String username,
            @Context UriInfo uriInfo) {
        PageRequestDTO pagination = QueryParameters.pagination(page, size);
        return userService.list(pagination, QueryParameters.optionalText(
                username, "username", uriInfo.getQueryParameters().containsKey("username")));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a user", description = "Fully replaces the editable username and display name. Server-controlled fields are unchanged.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "User updated"),
            @APIResponse(responseCode = "400", description = "Invalid UUID or request body", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "USER_NOT_FOUND", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "409", description = "USERNAME_ALREADY_EXISTS", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "415", description = "Content-Type is not application/json"),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public UserDTO update(
            @Parameter(description = "User identifier", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("id") String id,
            @RequestBody(required = true, description = "Complete set of editable user fields")
            @Valid @NotNull(message = "Request body is required") UpdateUserRequest request) {
        return userService.update(parseUserId(id), request);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a user", description = "Deletes a user and its membership and role-assignment links.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "User deleted; no response body"),
            @APIResponse(responseCode = "400", description = "Invalid UUID", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "USER_NOT_FOUND", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public Response delete(
            @Parameter(description = "User identifier", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("id") String id) {
        userService.delete(parseUserId(id));
        return Response.noContent().build();
    }

    @PUT
    @Path("/{userId}/roles/{roleId}")
    @Operation(summary = "Assign a role to a user", description = "Idempotently assigns a role. The user must already belong to the role's guild.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Role assigned or assignment already present; no response body"),
            @APIResponse(responseCode = "400", description = "Invalid UUID", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "USER_NOT_FOUND or ROLE_NOT_FOUND", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "409", description = "USER_NOT_GUILD_MEMBER", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public Response assignRole(
            @Parameter(description = "User receiving the role", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("userId") String userId,
            @Parameter(description = "Role to assign", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("roleId") String roleId) {
        associationService.assignRole(parseUserId(userId), parseRoleId(roleId));
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{userId}/roles/{roleId}")
    @Operation(summary = "Remove a role from a user", description = "Idempotently removes a role assignment.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Role removed or assignment already absent; no response body"),
            @APIResponse(responseCode = "400", description = "Invalid UUID", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "USER_NOT_FOUND or ROLE_NOT_FOUND", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public Response removeRole(
            @Parameter(description = "User losing the role", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("userId") String userId,
            @Parameter(description = "Role to remove", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("roleId") String roleId) {
        associationService.removeRole(parseUserId(userId), parseRoleId(roleId));
        return Response.noContent().build();
    }

    @GET
    @Path("/{userId}/roles")
    @Operation(summary = "List a user's roles", description = "Returns roles assigned to the user, ordered by position, then role UUID. "
            + "Empty and out-of-range pages return an empty items array.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated roles"),
            @APIResponse(responseCode = "400", description = "Invalid UUID or pagination", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "404", description = "USER_NOT_FOUND", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class))),
            @APIResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    schema = @Schema(implementation = org.acme.discord.application.dto.ErrorDTO.class)))
    })
    public PageDTO<RoleDTO> listRoles(
            @Parameter(description = "User whose roles are listed", required = true,
                    schema = @Schema(type = SchemaType.STRING, format = "uuid"))
            @PathParam("userId") String userId,
            @Parameter(description = "Zero-based page index", schema = @Schema(type = SchemaType.INTEGER,
                    defaultValue = "0", minimum = "0"))
            @DefaultValue(QueryParameters.DEFAULT_PAGE) @QueryParam("page") String page,
            @Parameter(description = "Page size from 1 to 100", schema = @Schema(type = SchemaType.INTEGER,
                    defaultValue = "20", minimum = "1", maximum = "100"))
            @DefaultValue(QueryParameters.DEFAULT_SIZE) @QueryParam("size") String size) {
        return associationService.listRoles(
                parseUserId(userId), QueryParameters.pagination(page, size));
    }

    private static UUID parseUserId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Invalid user id");
        }
    }

    private static UUID parseRoleId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Invalid role id");
        }
    }

}
