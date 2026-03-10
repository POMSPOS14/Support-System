package com.company.support.incident.service;

import com.company.support.incident.dto.request.CreateIncidentRequest;
import com.company.support.incident.dto.request.UpdateIncidentRequest;
import com.company.support.incident.dto.response.IncidentDetailResponse;
import com.company.support.incident.dto.response.IncidentResponse;
import com.company.support.incident.entity.*;
import com.company.support.incident.mapper.IncidentMapper;
import com.company.support.incident.repository.IncidentRepository;
import com.company.support.image.grpc.GetImagesByIncidentIdRequest;
import com.company.support.image.grpc.MutinyImageServiceGrpc;
import com.company.support.user.grpc.GetUserByIdRequest;
import com.company.support.user.grpc.MutinyUserServiceGrpc;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
public class IncidentService {

    @Inject
    IncidentRepository incidentRepository;

    @Inject
    IncidentMapper incidentMapper;

    @GrpcClient("user-service")
    MutinyUserServiceGrpc.MutinyUserServiceStub userGrpcClient;

    @GrpcClient("image-service")
    MutinyImageServiceGrpc.MutinyImageServiceStub imageGrpcClient;

    @Channel("incident-events")
    Emitter<String> incidentEventEmitter;

    @WithSession
    public Uni<List<IncidentResponse>> findAll() {
        return incidentRepository.listAll()
                .map(incidentMapper::toResponseList);
    }

    @WithSession
    public Uni<IncidentDetailResponse> findById(Long id) {
        return incidentRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Incident not found: " + id))
                .flatMap(incident -> {
                    var detail = incidentMapper.toDetailResponse(incident);

                    // Получаем initiator через gRPC реактивно
                    Uni<Void> initiatorUni = userGrpcClient.getUserById(
                                    GetUserByIdRequest.newBuilder().setId(incident.getInitiatorId()).build())
                            .onItem().invoke(user -> detail.setInitiator(incidentMapper.fromUserProto(user)))
                            .replaceWithVoid();

                    // Получаем analyst через gRPC реактивно (если назначен)
                    Uni<Void> analystUni = incident.getAnalystId() != null
                            ? userGrpcClient.getUserById(
                                    GetUserByIdRequest.newBuilder().setId(incident.getAnalystId()).build())
                            .onItem().invoke(user -> detail.setAnalyst(incidentMapper.fromUserProto(user)))
                            .replaceWithVoid()
                            : Uni.createFrom().voidItem();

                    // Получаем images через gRPC реактивно
                    Uni<Void> imagesUni = imageGrpcClient.getImagesByIncidentId(
                                    GetImagesByIncidentIdRequest.newBuilder().setIncidentId(id).build())
                            .onItem().invoke(resp -> detail.setImages(incidentMapper.fromImageProtoList(resp.getImagesList())))
                            .replaceWithVoid();

                    // Запускаем все три параллельно и возвращаем detail
                    return Uni.join().all(initiatorUni, analystUni, imagesUni).andFailFast()
                            .replaceWith(detail);
                });
    }

    @WithTransaction
    public Uni<IncidentResponse> create(CreateIncidentRequest request, Long initiatorId) {
        var incident = incidentMapper.toEntity(request);
        incident.setInitiatorId(initiatorId);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setDateCreate(LocalDateTime.now());

        return incidentRepository.persistAndFlush(incident)
                .map(saved -> {
                    incidentEventEmitter.send(buildIncidentCreatedEvent(saved));
                    return incidentMapper.toResponse(saved);
                });
    }

    @WithTransaction
    public Uni<IncidentResponse> update(Long id, UpdateIncidentRequest request) {
        return incidentRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Incident not found: " + id))
                .flatMap(incident -> {
                    incidentMapper.updateEntity(request, incident);
                    return incidentRepository.persistAndFlush(incident);
                })
                .map(incidentMapper::toResponse);
    }

    @WithTransaction
    public Uni<Void> delete(Long id) {
        return incidentRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Incident not found: " + id))
                .flatMap(incident -> {
                    incidentEventEmitter.send(buildIncidentDeletedEvent(id));
                    return incidentRepository.delete(incident);
                });
    }

    @WithTransaction
    public Uni<IncidentResponse> assignAnalyst(Long id, Long analystId) {
        return incidentRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Incident not found: " + id))
                .flatMap(incident -> {
                    incident.setAnalystId(analystId);
                    return incidentRepository.persistAndFlush(incident);
                })
                .map(incidentMapper::toResponse);
    }

    @WithTransaction
    public Uni<IncidentResponse> assignStatus(Long id, IncidentStatus status) {
        return incidentRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Incident not found: " + id))
                .flatMap(incident -> {
                    incident.setStatus(status);
                    if (status == IncidentStatus.CLOSED) {
                        incident.setDateClosed(LocalDateTime.now());
                    }
                    return incidentRepository.persistAndFlush(incident);
                })
                .map(incidentMapper::toResponse);
    }

    @WithTransaction
    public Uni<IncidentResponse> assignPriority(Long id, IncidentPriority priority) {
        return incidentRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Incident not found: " + id))
                .flatMap(incident -> {
                    incident.setPriority(priority);
                    return incidentRepository.persistAndFlush(incident);
                })
                .map(incidentMapper::toResponse);
    }

    @WithTransaction
    public Uni<IncidentResponse> assignCategory(Long id, IncidentCategory category) {
        return incidentRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Incident not found: " + id))
                .flatMap(incident -> {
                    incident.setCategory(category);
                    return incidentRepository.persistAndFlush(incident);
                })
                .map(incidentMapper::toResponse);
    }

    @WithTransaction
    public Uni<IncidentResponse> assignResponsibleService(Long id, ResponsibleService service) {
        return incidentRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Incident not found: " + id))
                .flatMap(incident -> {
                    incident.setResponsibleService(service);
                    return incidentRepository.persistAndFlush(incident);
                })
                .map(incidentMapper::toResponse);
    }

    // --- Kafka event builders ---

    private String buildIncidentCreatedEvent(Incident incident) {
        return String.format("""
                {"eventType":"INCIDENT_CREATED","incidentId":%d,"incidentName":"%s"}
                """, incident.getId(), incident.getName()).strip();
    }

    private String buildIncidentDeletedEvent(Long incidentId) {
        return String.format("""
                {"eventType":"INCIDENT_DELETED","incidentId":%d}
                """, incidentId).strip();
    }
}