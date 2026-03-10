package com.company.support.incident.resource;

import com.company.support.incident.dto.request.*;
import com.company.support.incident.dto.response.IncidentDetailResponse;
import com.company.support.incident.dto.response.IncidentResponse;
import com.company.support.incident.entity.*;
import com.company.support.incident.service.IncidentService;
import com.company.support.image.grpc.MutinyImageServiceGrpc;
import com.company.support.image.grpc.UploadImageRequest;
import com.company.support.image.grpc.DeleteImageRequest;
import com.company.support.image.grpc.DownloadImageRequest;
import com.company.support.incident.dto.response.ImageResponse;
import com.company.support.user.grpc.GetUserByKeycloakIdRequest;
import com.company.support.user.grpc.MutinyUserServiceGrpc;
import com.google.protobuf.ByteString;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/incidents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Incidents", description = "Incident management API")
public class IncidentResource {

    @Inject
    IncidentService incidentService;

    @Inject
    JsonWebToken jwt;

    @GrpcClient("user-service")
    MutinyUserServiceGrpc.MutinyUserServiceStub userGrpcClient;

    @GrpcClient("image-service")
    MutinyImageServiceGrpc.MutinyImageServiceStub imageGrpcClient;

    @GET
    @RolesAllowed({"admin", "analyst", "user"})
    @Operation(summary = "Get all incidents")
    public Uni<List<IncidentResponse>> getAll() {
        return incidentService.findAll();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"admin", "analyst", "user"})
    @Operation(summary = "Get incident with full details")
    public Uni<IncidentDetailResponse> getById(@PathParam("id") Long id) {
        return incidentService.findById(id);
    }

    @POST
    @RolesAllowed({"admin", "analyst", "user"})
    @Operation(summary = "Create a new incident")
    public Uni<Response> create(@Valid CreateIncidentRequest request) {
        String keycloakSub = jwt.getSubject(); // UUID пользователя в Keycloak (поле sub)
        return userGrpcClient.getUserByKeycloakId(
                        GetUserByKeycloakIdRequest.newBuilder().setKeycloakId(keycloakSub).build())
                .flatMap(user -> incidentService.create(request, user.getId()))
                .map(r -> Response.status(Response.Status.CREATED).entity(r).build());
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"admin", "analyst", "user"})
    @Operation(summary = "Update incident")
    public Uni<IncidentResponse> update(@PathParam("id") Long id, @Valid UpdateIncidentRequest request) {
        return incidentService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"admin", "analyst"})
    @Operation(summary = "Delete incident")
    public Uni<Response> delete(@PathParam("id") Long id) {
        return incidentService.delete(id)
                .map(v -> Response.noContent().build());
    }

    @PATCH
    @Path("/{id}/analyst")
    @RolesAllowed({"admin", "analyst"})
    @Operation(summary = "Assign analyst to incident")
    public Uni<IncidentResponse> assignAnalyst(@PathParam("id") Long id, AssignAnalystRequest request) {
        return incidentService.assignAnalyst(id, request.getAnalystId());
    }

    @PATCH
    @Path("/{id}/status")
    @RolesAllowed({"admin", "analyst"})
    @Operation(summary = "Change incident status")
    public Uni<IncidentResponse> assignStatus(@PathParam("id") Long id, AssignStatusRequest request) {
        return incidentService.assignStatus(id, request.getStatus());
    }

    @PATCH
    @Path("/{id}/priority")
    @RolesAllowed({"admin", "analyst"})
    @Operation(summary = "Set incident priority")
    public Uni<IncidentResponse> assignPriority(@PathParam("id") Long id, AssignPriorityRequest request) {
        return incidentService.assignPriority(id, request.getPriority());
    }

    @PATCH
    @Path("/{id}/category")
    @RolesAllowed({"admin", "analyst"})
    @Operation(summary = "Set incident category")
    public Uni<IncidentResponse> assignCategory(@PathParam("id") Long id, AssignCategoryRequest request) {
        return incidentService.assignCategory(id, request.getCategory());
    }

    @PATCH
    @Path("/{id}/responsible-service")
    @RolesAllowed({"admin", "analyst"})
    @Operation(summary = "Set responsible service")
    public Uni<IncidentResponse> assignResponsibleService(@PathParam("id") Long id, AssignResponsibleServiceRequest request) {
        return incidentService.assignResponsibleService(id, request.getResponsibleService());
    }

    @POST
    @Path("/{id}/images")
    @RolesAllowed({"admin", "analyst", "user"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload image for incident")
    public Uni<Response> uploadImage(@PathParam("id") Long id,
                                     @RestForm("file") FileUpload file) {
        return Uni.createFrom().item(() -> {
                    try {
                        return java.nio.file.Files.readAllBytes(file.uploadedFile());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to read file", e);
                    }
                })
                .flatMap(bytes -> imageGrpcClient.uploadImage(
                        UploadImageRequest.newBuilder()
                                .setIncidentId(id)
                                .setFileName(file.fileName())
                                .setContentType(file.contentType() != null ? file.contentType() : "application/octet-stream")
                                .setData(ByteString.copyFrom(bytes))
                                .build()))
                .map(r -> Response.status(Response.Status.CREATED).entity(
                        new ImageResponse(r.getId(), r.getIncidentId(), r.getUrl(),
                                r.getFileName(), r.getSize(), r.getMediaType())).build());
    }

    @DELETE
    @Path("/{id}/images/{imageId}")
    @RolesAllowed({"admin", "analyst"})
    @Operation(summary = "Delete image from incident")
    public Uni<Response> deleteImage(@PathParam("id") Long id, @PathParam("imageId") Long imageId) {
        return imageGrpcClient.deleteImage(
                        DeleteImageRequest.newBuilder().setId(imageId).build())
                .map(r -> r.getSuccess()
                        ? Response.noContent().build()
                        : Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/{id}/images/{imageId}/download")
    @RolesAllowed({"admin", "analyst", "user"})
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "Download image from incident")
    public Uni<Response> downloadImage(@PathParam("id") Long id, @PathParam("imageId") Long imageId) {
        return imageGrpcClient.downloadImage(
                        DownloadImageRequest.newBuilder().setId(imageId).build())
                .map(r -> Response.ok(r.getData().toByteArray())
                        .header("Content-Disposition", "attachment; filename=\"" + r.getFileName() + "\"")
                        .header("Content-Type", r.getContentType())
                        .build());
    }
}