package com.company.support.image.consumer;

import com.company.support.image.service.ImageService;
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
    ImageService imageService;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("incident-events")
    public Uni<Void> consume(String message) {
        LOG.infof("Received Kafka event: %s", message);
        try {
            var node = objectMapper.readTree(message);
            String eventType = node.get("eventType").asText();

            if ("INCIDENT_DELETED".equals(eventType)) {
                Long incidentId = node.get("incidentId").asLong();
                return imageService.deleteAllByIncidentId(incidentId);
            }

            return Uni.createFrom().voidItem();

        } catch (Exception e) {
            LOG.errorf(e, "Failed to process Kafka message: %s", message);
            return Uni.createFrom().voidItem();
        }
    }
}
