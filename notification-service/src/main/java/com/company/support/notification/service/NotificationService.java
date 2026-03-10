package com.company.support.notification.service;

import com.company.support.notification.dto.IncidentEvent;
import com.company.support.user.grpc.GetUsersByRoleRequest;
import com.company.support.user.grpc.MutinyUserServiceGrpc;
import com.company.support.user.grpc.UserResponse;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);

    @Inject
    ReactiveMailer mailer;

    @GrpcClient("user-service")
    MutinyUserServiceGrpc.MutinyUserServiceStub userGrpcClient;

    public Uni<Void> notifyAdminsAboutNewIncident(IncidentEvent event) {
        LOG.infof("Notifying admins about new incident #%d '%s'", event.getIncidentId(), event.getIncidentName());

        return userGrpcClient.getUsersByRole(
                        GetUsersByRoleRequest.newBuilder().setRole("ADMIN").build())
                .flatMap(response -> {
                    List<UserResponse> admins = response.getUsersList();

                    if (admins.isEmpty()) {
                        LOG.warn("No admins found, skipping notification");
                        return Uni.createFrom().voidItem();
                    }

                    List<Mail> mails = admins.stream()
                            .filter(admin -> admin.getEmail() != null && !admin.getEmail().isBlank())
                            .map(admin -> Mail.withHtml(
                                    admin.getEmail(),
                                    "New incident #" + event.getIncidentId(),
                                    buildEmailBody(event, admin)))
                            .toList();

                    if (mails.isEmpty()) {
                        LOG.warn("Admins found but none have email, skipping notification");
                        return Uni.createFrom().voidItem();
                    }

                    LOG.infof("Sending notification to %d admin(s)", mails.size());
                    return mailer.send(mails.toArray(new Mail[0]));
                })
                .onFailure().invoke(e ->
                        LOG.errorf(e, "Failed to notify admins about incident #%d", event.getIncidentId()))
                .onFailure().recoverWithNull()
                .replaceWithVoid();
    }

    private String buildEmailBody(IncidentEvent event, UserResponse admin) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <h2 style="color: #c0392b;">New incident in support system</h2>
                    <p>Dear <b>%s</b>,</p>
                    <p>A new incident has been registered and requires your attention:</p>
                    <table border="1" cellpadding="8" cellspacing="0" style="border-collapse: collapse;">
                        <tr><td><b>ID</b></td><td>#%d</td></tr>
                        <tr><td><b>Name</b></td><td>%s</td></tr>
                    </table>
                    <p>Please log in to the system to process the incident.</p>
                </body>
                </html>
                """.formatted(admin.getUserFullName(), event.getIncidentId(), event.getIncidentName());
    }
}