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
import org.acme.discord.application.dto.RoleDTO;
import org.acme.discord.application.dto.usecases.CreateRoleRequest;
import org.acme.discord.application.dto.usecases.UpdateRoleRequest;

import java.net.URI;
import java.util.UUID;

/**
 * REST resource exposing Role CRUD operations.
 */
@Path("/api/roles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RoleResource {

    private final RoleService roleService;

    @Inject
    public RoleResource(RoleService roleService) {
        this.roleService = roleService;
    }

    @POST
    public Response create(
            @Valid @NotNull(message = "Request body is required") CreateRoleRequest request,
            @Context UriInfo uriInfo) {
        RoleDTO role = roleService.create(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(role.id().toString()).build();
        return Response.created(location).entity(role).build();
    }

    @GET
    @Path("/{id}")
    public RoleDTO get(@PathParam("id") String id) {
        return roleService.get(parseRoleId(id));
    }

    @GET
    public PageDTO<RoleDTO> list(
            @DefaultValue("0") @QueryParam("page") String page,
            @DefaultValue("20") @QueryParam("size") String size) {
        return roleService.list(parseInteger(page, "page"), parseInteger(size, "size"));
    }

    @PUT
    @Path("/{id}")
    public RoleDTO update(
            @PathParam("id") String id,
            @Valid @NotNull(message = "Request body is required") UpdateRoleRequest request) {
        return roleService.update(parseRoleId(id), request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        roleService.delete(parseRoleId(id));
        return Response.noContent().build();
    }

    private static UUID parseRoleId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid role id");
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
