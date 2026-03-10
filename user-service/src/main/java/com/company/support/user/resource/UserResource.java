package com.company.support.user.resource;

import com.company.support.user.dto.request.CreateUserRequest;
import com.company.support.user.dto.request.UpdateUserRequest;
import com.company.support.user.dto.response.UserResponse;
import com.company.support.user.entity.UserRole;
import com.company.support.user.service.UserService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "User management API")
public class UserResource {

    @Inject
    UserService userService;

    @GET
    @RolesAllowed({"admin"})
    @Operation(summary = "Get all users")
    public Uni<List<UserResponse>> getAll() {
        return userService.findAll();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"admin", "analyst"})
    @Operation(summary = "Get user by ID")
    public Uni<UserResponse> getById(@PathParam("id") Long id) {
        return userService.findById(id);
    }

    @GET
    @Path("/role/{role}")
    @RolesAllowed({"admin", "analyst"})
    @Operation(summary = "Get users by role")
    public Uni<List<UserResponse>> getByRole(@PathParam("role") UserRole role) {
        return userService.findByRole(role);
    }

    @POST
    @RolesAllowed({"admin"})
    @Operation(summary = "Create user")
    public Uni<Response> create(@Valid CreateUserRequest request) {
        return userService.create(request)
                .map(r -> Response.status(Response.Status.CREATED).entity(r).build());
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"admin"})
    @Operation(summary = "Update user")
    public Uni<UserResponse> update(@PathParam("id") Long id, @Valid UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"admin"})
    @Operation(summary = "Delete user")
    public Uni<Response> delete(@PathParam("id") Long id) {
        return userService.delete(id)
                .map(v -> Response.noContent().build());
    }
}
