package com.company.support.notification.consumer;

import com.company.support.notification.dto.IncidentEvent;
import com.company.support.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentEventConsumerTest {

    @Mock
    NotificationService notificationService;

    // Используем реальный ObjectMapper — он не имеет внешних зависимостей
    IncidentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new IncidentEventConsumer(notificationService, new ObjectMapper());
    }

    @Test
    void consume_shouldCallNotifyOnIncidentCreated() {
        var message = "{\"eventType\":\"INCIDENT_CREATED\",\"incidentId\":5,\"incidentName\":\"Server down\"}";
        when(notificationService.notifyAdminsAboutNewIncident(any())).thenReturn(Uni.createFrom().voidItem());

        consumer.consume(message).await().indefinitely();

        var captor = ArgumentCaptor.forClass(IncidentEvent.class);
        verify(notificationService).notifyAdminsAboutNewIncident(captor.capture());
        assertThat(captor.getValue().getIncidentId()).isEqualTo(5L);
        assertThat(captor.getValue().getIncidentName()).isEqualTo("Server down");
    }

    @Test
    void consume_shouldIgnoreUnknownEventTypes() {
        var message = "{\"eventType\":\"INCIDENT_DELETED\",\"incidentId\":5}";

        consumer.consume(message).await().indefinitely();

        verify(notificationService, never()).notifyAdminsAboutNewIncident(any());
    }

    @Test
    void consume_shouldHandleInvalidJsonGracefully() {
        consumer.consume("not-a-json").await().indefinitely();

        verify(notificationService, never()).notifyAdminsAboutNewIncident(any());
    }

    @Test
    void consume_shouldHandleEmptyEventTypeGracefully() {
        consumer.consume("{}").await().indefinitely();

        verify(notificationService, never()).notifyAdminsAboutNewIncident(any());
    }
}