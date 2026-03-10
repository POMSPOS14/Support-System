package com.company.support.notification.consumer;

import com.company.support.notification.dto.IncidentEvent;
import com.company.support.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class IncidentEventConsumer {

    private static final Logger LOG = Logger.getLogger(IncidentEventConsumer.class);

    @Inject
    NotificationService notificationService;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("incident-events")
    public Uni<Void> consume(String message) {
        LOG.infof("Received Kafka event: %s", message);
        try {
            IncidentEvent event = objectMapper.readValue(message, IncidentEvent.class);

            return switch (event.getEventType()) {
                case "INCIDENT_CREATED" -> notificationService.notifyAdminsAboutNewIncident(event);
                default -> {
                    LOG.infof("Ignoring event type: %s", event.getEventType());
                    yield Uni.createFrom().voidItem();
                }
            };
        } catch (Exception e) {
            LOG.errorf(e, "Failed to parse Kafka message: %s", message);
            return Uni.createFrom().voidItem();
        }
    }
}
