package com.company.support.incident.service;

import com.company.support.incident.dto.request.CreateIncidentRequest;
import com.company.support.incident.dto.request.UpdateIncidentRequest;
import com.company.support.incident.dto.response.IncidentResponse;
import com.company.support.incident.entity.*;
import com.company.support.incident.mapper.IncidentMapper;
import com.company.support.incident.repository.IncidentRepository;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IncidentServiceTest {

    @Mock
    IncidentRepository incidentRepository;

    @Mock
    IncidentMapper incidentMapper;

    @Mock
    Emitter<String> incidentEventEmitter;

    @InjectMocks
    IncidentService incidentService;

    // --- create ---

    @Test
    void create_shouldPersistIncidentWithOpenStatusAndInitiatorId() {
        var request = new CreateIncidentRequest("Test incident", "Some description");
        Long initiatorId = 42L;
        var savedIncident = Incident.builder()
                .id(1L).name("Test incident").status(IncidentStatus.OPEN).initiatorId(initiatorId).build();
        var expectedResponse = IncidentResponse.builder()
                .id(1L).name("Test incident").status(IncidentStatus.OPEN).initiatorId(initiatorId).build();

        when(incidentMapper.toEntity(request)).thenReturn(savedIncident);
        when(incidentRepository.persistAndFlush(any(Incident.class))).thenReturn(Uni.createFrom().item(savedIncident));
        when(incidentMapper.toResponse(any(Incident.class))).thenReturn(expectedResponse);
        when(incidentEventEmitter.send(anyString())).thenReturn(CompletableFuture.completedFuture(null));

        var result = incidentService.create(request, initiatorId).await().indefinitely();

        assertThat(result.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(result.getInitiatorId()).isEqualTo(initiatorId);
        verify(incidentRepository).persistAndFlush(any(Incident.class));
        verify(incidentEventEmitter).send(anyString());
    }

    @Test
    void create_shouldSetInitiatorIdFromParameter() {
        Long initiatorId = 99L;
        var request = new CreateIncidentRequest("Incident", "Desc");
        var entity = Incident.builder().id(1L).initiatorId(initiatorId).status(IncidentStatus.OPEN).build();
        var response = IncidentResponse.builder().id(1L).initiatorId(initiatorId).build();

        when(incidentMapper.toEntity(request)).thenReturn(entity);
        when(incidentRepository.persistAndFlush(any(Incident.class))).thenReturn(Uni.createFrom().item(entity));
        when(incidentMapper.toResponse(any(Incident.class))).thenReturn(response);
        when(incidentEventEmitter.send(anyString())).thenReturn(CompletableFuture.completedFuture(null));

        var result = incidentService.create(request, initiatorId).await().indefinitely();

        assertThat(result.getInitiatorId()).isEqualTo(initiatorId);
    }

    // --- update ---

    @Test
    void update_shouldCallUpdateEntityOnMapper() {
        Long incidentId = 1L;
        var request = new UpdateIncidentRequest("New name", "New desc");
        var incident = Incident.builder().id(incidentId).name("Old name").initiatorId(1L).build();
        var response = IncidentResponse.builder().id(incidentId).name("New name").build();

        when(incidentRepository.findById(incidentId)).thenReturn(Uni.createFrom().item(incident));
        when(incidentRepository.persistAndFlush(any(Incident.class))).thenReturn(Uni.createFrom().item(incident));
        when(incidentMapper.toResponse(any(Incident.class))).thenReturn(response);

        var result = incidentService.update(incidentId, request).await().indefinitely();

        assertThat(result.getId()).isEqualTo(incidentId);
        verify(incidentMapper).updateEntity(eq(request), eq(incident));
    }

    @Test
    void update_shouldThrowNotFoundWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(999L)).thenReturn(Uni.createFrom().nullItem());

        assertThatThrownBy(() ->
                incidentService.update(999L, new UpdateIncidentRequest("x", "y")).await().indefinitely())
                .isInstanceOf(NotFoundException.class);
    }

    // --- delete ---

    @Test
    void delete_shouldSendKafkaEventAndDeleteIncident() {
        Long incidentId = 1L;
        var incident = Incident.builder().id(incidentId).initiatorId(1L).build();

        when(incidentRepository.findById(incidentId)).thenReturn(Uni.createFrom().item(incident));
        when(incidentRepository.delete(incident)).thenReturn(Uni.createFrom().voidItem());
        when(incidentEventEmitter.send(anyString())).thenReturn(CompletableFuture.completedFuture(null));

        incidentService.delete(incidentId).await().indefinitely();

        verify(incidentEventEmitter).send(anyString());
        verify(incidentRepository).delete(incident);
    }

    @Test
    void delete_shouldThrowNotFoundWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(999L)).thenReturn(Uni.createFrom().nullItem());

        assertThatThrownBy(() ->
                incidentService.delete(999L).await().indefinitely())
                .isInstanceOf(NotFoundException.class);
    }

    // --- assignStatus ---

    @Test
    void assignStatus_shouldSetDateClosedWhenStatusIsClosed() {
        Long incidentId = 1L;
        var incident = Incident.builder().id(incidentId).status(IncidentStatus.OPEN).initiatorId(1L).build();

        when(incidentRepository.findById(incidentId)).thenReturn(Uni.createFrom().item(incident));
        when(incidentRepository.persistAndFlush(any(Incident.class))).thenReturn(Uni.createFrom().item(incident));
        when(incidentMapper.toResponse(any(Incident.class))).thenReturn(
                IncidentResponse.builder().id(incidentId).status(IncidentStatus.CLOSED).build());

        var result = incidentService.assignStatus(incidentId, IncidentStatus.CLOSED).await().indefinitely();

        assertThat(result.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(incident.getDateClosed()).isNotNull();
    }

    @Test
    void assignStatus_shouldNotSetDateClosedWhenStatusIsNotClosed() {
        Long incidentId = 1L;
        var incident = Incident.builder().id(incidentId).status(IncidentStatus.OPEN).initiatorId(1L).build();

        when(incidentRepository.findById(incidentId)).thenReturn(Uni.createFrom().item(incident));
        when(incidentRepository.persistAndFlush(any(Incident.class))).thenReturn(Uni.createFrom().item(incident));
        when(incidentMapper.toResponse(any(Incident.class))).thenReturn(IncidentResponse.builder().build());

        incidentService.assignStatus(incidentId, IncidentStatus.WAITING_FOR_INFO).await().indefinitely();

        assertThat(incident.getDateClosed()).isNull();
    }

    // --- assignAnalyst ---

    @Test
    void assignAnalyst_shouldSetAnalystId() {
        Long incidentId = 1L;
        Long analystId = 5L;
        var incident = Incident.builder().id(incidentId).initiatorId(1L).build();
        var response = IncidentResponse.builder().id(incidentId).analystId(analystId).build();

        when(incidentRepository.findById(incidentId)).thenReturn(Uni.createFrom().item(incident));
        when(incidentRepository.persistAndFlush(any(Incident.class))).thenReturn(Uni.createFrom().item(incident));
        when(incidentMapper.toResponse(any(Incident.class))).thenReturn(response);

        var result = incidentService.assignAnalyst(incidentId, analystId).await().indefinitely();

        assertThat(incident.getAnalystId()).isEqualTo(analystId);
        assertThat(result.getAnalystId()).isEqualTo(analystId);
    }

    // --- assignPriority ---

    @Test
    void assignPriority_shouldSetPriority() {
        Long incidentId = 1L;
        var incident = Incident.builder().id(incidentId).initiatorId(1L).build();
        var response = IncidentResponse.builder().id(incidentId).priority(IncidentPriority.HIGH).build();

        when(incidentRepository.findById(incidentId)).thenReturn(Uni.createFrom().item(incident));
        when(incidentRepository.persistAndFlush(any(Incident.class))).thenReturn(Uni.createFrom().item(incident));
        when(incidentMapper.toResponse(any(Incident.class))).thenReturn(response);

        var result = incidentService.assignPriority(incidentId, IncidentPriority.HIGH).await().indefinitely();

        assertThat(incident.getPriority()).isEqualTo(IncidentPriority.HIGH);
        assertThat(result.getPriority()).isEqualTo(IncidentPriority.HIGH);
    }

    // --- assignCategory ---

    @Test
    void assignCategory_shouldSetCategory() {
        Long incidentId = 1L;
        var incident = Incident.builder().id(incidentId).initiatorId(1L).build();
        var response = IncidentResponse.builder().id(incidentId).category(IncidentCategory.SOFTWARE).build();

        when(incidentRepository.findById(incidentId)).thenReturn(Uni.createFrom().item(incident));
        when(incidentRepository.persistAndFlush(any(Incident.class))).thenReturn(Uni.createFrom().item(incident));
        when(incidentMapper.toResponse(any(Incident.class))).thenReturn(response);

        var result = incidentService.assignCategory(incidentId, IncidentCategory.SOFTWARE).await().indefinitely();

        assertThat(incident.getCategory()).isEqualTo(IncidentCategory.SOFTWARE);
        assertThat(result.getCategory()).isEqualTo(IncidentCategory.SOFTWARE);
    }

    // --- assignResponsibleService ---

    @Test
    void assignResponsibleService_shouldSetResponsibleService() {
        Long incidentId = 1L;
        var incident = Incident.builder().id(incidentId).initiatorId(1L).build();
        var response = IncidentResponse.builder().id(incidentId).responsibleService(ResponsibleService.BACKEND).build();

        when(incidentRepository.findById(incidentId)).thenReturn(Uni.createFrom().item(incident));
        when(incidentRepository.persistAndFlush(any(Incident.class))).thenReturn(Uni.createFrom().item(incident));
        when(incidentMapper.toResponse(any(Incident.class))).thenReturn(response);

        var result = incidentService.assignResponsibleService(incidentId, ResponsibleService.BACKEND).await().indefinitely();

        assertThat(incident.getResponsibleService()).isEqualTo(ResponsibleService.BACKEND);
        assertThat(result.getResponsibleService()).isEqualTo(ResponsibleService.BACKEND);
    }

    // --- findAll ---

    @Test
    void findAll_shouldReturnAllIncidents() {
        var incidents = List.of(
                Incident.builder().id(1L).name("Inc 1").initiatorId(1L).build(),
                Incident.builder().id(2L).name("Inc 2").initiatorId(2L).build()
        );
        var responses = List.of(
                IncidentResponse.builder().id(1L).name("Inc 1").build(),
                IncidentResponse.builder().id(2L).name("Inc 2").build()
        );

        when(incidentRepository.listAll()).thenReturn(Uni.createFrom().item(incidents));
        when(incidentMapper.toResponseList(incidents)).thenReturn(responses);

        var result = incidentService.findAll().await().indefinitely();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Inc 1");
    }
}