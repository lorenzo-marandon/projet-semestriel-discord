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

import java.net.URI;
import java.util.UUID;

/**
 * REST resource exposing User CRUD operations.
 */
@Path("/api/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    private final UserService userService;
    private final AssociationService associationService;

    @Inject
    public UserResource(UserService userService, AssociationService associationService) {
        this.userService = userService;
        this.associationService = associationService;
    }

    @POST
    public Response create(
            @Valid @NotNull(message = "Request body is required") CreateUserRequest request,
            @Context UriInfo uriInfo) {
        UserDTO user = userService.create(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(user.id().toString()).build();
        return Response.created(location).entity(user).build();
    }

    @GET
    @Path("/{id}")
    public UserDTO get(@PathParam("id") String id) {
        return userService.get(parseUserId(id));
    }

    @GET
    public PageDTO<UserDTO> list(
            @DefaultValue(QueryParameters.DEFAULT_PAGE) @QueryParam("page") String page,
            @DefaultValue(QueryParameters.DEFAULT_SIZE) @QueryParam("size") String size,
            @QueryParam("username") String username,
            @Context UriInfo uriInfo) {
        PageRequestDTO pagination = QueryParameters.pagination(page, size);
        return userService.list(pagination, QueryParameters.optionalText(
                username, "username", uriInfo.getQueryParameters().containsKey("username")));
    }

    @PUT
    @Path("/{id}")
    public UserDTO update(
            @PathParam("id") String id,
            @Valid @NotNull(message = "Request body is required") UpdateUserRequest request) {
        return userService.update(parseUserId(id), request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        userService.delete(parseUserId(id));
        return Response.noContent().build();
    }

    @PUT
    @Path("/{userId}/roles/{roleId}")
    public Response assignRole(
            @PathParam("userId") String userId,
            @PathParam("roleId") String roleId) {
        associationService.assignRole(parseUserId(userId), parseRoleId(roleId));
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{userId}/roles/{roleId}")
    public Response removeRole(
            @PathParam("userId") String userId,
            @PathParam("roleId") String roleId) {
        associationService.removeRole(parseUserId(userId), parseRoleId(roleId));
        return Response.noContent().build();
    }

    @GET
    @Path("/{userId}/roles")
    public PageDTO<RoleDTO> listRoles(
            @PathParam("userId") String userId,
            @DefaultValue(QueryParameters.DEFAULT_PAGE) @QueryParam("page") String page,
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
