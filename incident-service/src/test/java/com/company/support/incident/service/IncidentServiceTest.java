package com.company.support.incident.service;

import com.company.support.incident.dto.request.CreateIncidentRequest;
import com.company.support.incident.dto.request.UpdateIncidentRequest;
import com.company.support.incident.dto.response.IncidentResponse;
import com.company.support.incident.entity.Incident;
import com.company.support.incident.entity.IncidentStatus;
import com.company.support.incident.mapper.IncidentMapper;
import com.company.support.incident.repository.IncidentRepository;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void create_shouldPersistIncidentWithOpenStatusAndInitiatorId() {
        var request = new CreateIncidentRequest("Test incident", "Some description");
        Long initiatorId = 42L;

        var savedIncident = Incident.builder()
                .id(1L).name("Test incident").description("Some description")
                .status(IncidentStatus.OPEN).initiatorId(initiatorId).build();
        var expectedResponse = IncidentResponse.builder()
                .id(1L).name("Test incident").status(IncidentStatus.OPEN).initiatorId(initiatorId).build();

        when(incidentMapper.toEntity(request)).thenReturn(savedIncident);
        when(incidentRepository.persistAndFlush(any(Incident.class))).thenReturn(Uni.createFrom().item(savedIncident));
        when(incidentMapper.toResponse(any(Incident.class))).thenReturn(expectedResponse);
        when(incidentEventEmitter.send(anyString())).thenReturn(CompletableFuture.completedFuture(null));

        var result = incidentService.create(request, initiatorId).await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(result.getInitiatorId()).isEqualTo(initiatorId);
        verify(incidentRepository).persistAndFlush(any(Incident.class));
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

    @Test
    void assignStatus_shouldSetDateClosedWhenStatusIsClosed() {
        Long incidentId = 1L;
        var incident = Incident.builder().id(incidentId).name("Test").status(IncidentStatus.OPEN).initiatorId(1L).build();

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
}