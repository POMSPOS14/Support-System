package com.company.support.notification.service;

import com.company.support.notification.dto.IncidentEvent;
import com.company.support.user.grpc.UserResponse;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    ReactiveMailer mailer;

    // userGrpcClient не мокаем здесь — тестируем sendToAdmins напрямую
    @InjectMocks
    NotificationService notificationService;

    private IncidentEvent event(long id, String name) {
        var e = new IncidentEvent();
        e.setEventType("INCIDENT_CREATED");
        e.setIncidentId(id);
        e.setIncidentName(name);
        return e;
    }

    private UserResponse admin(String email, String fullName) {
        return UserResponse.newBuilder()
                .setEmail(email)
                .setUserFullName(fullName)
                .build();
    }

    // --- sendToAdmins ---

    @Test
    void sendToAdmins_shouldSendEmailToEachAdmin() {
        var event = event(1L, "Server down");
        var admins = List.of(
                admin("admin1@test.com", "Admin One"),
                admin("admin2@test.com", "Admin Two")
        );

        when(mailer.send(any(Mail[].class))).thenReturn(Uni.createFrom().voidItem());

        notificationService.sendToAdmins(event, admins).await().indefinitely();

        var captor = ArgumentCaptor.forClass(Mail[].class);
        verify(mailer).send(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()[0].getTo()).contains("admin1@test.com");
        assertThat(captor.getValue()[1].getTo()).contains("admin2@test.com");
    }

    @Test
    void sendToAdmins_shouldIncludeIncidentIdInSubject() {
        var event = event(42L, "DB crash");
        var admins = List.of(admin("admin@test.com", "Admin"));

        when(mailer.send(any(Mail[].class))).thenReturn(Uni.createFrom().voidItem());

        notificationService.sendToAdmins(event, admins).await().indefinitely();

        var captor = ArgumentCaptor.forClass(Mail[].class);
        verify(mailer).send(captor.capture());
        assertThat(captor.getValue()[0].getSubject()).contains("42");
    }

    @Test
    void sendToAdmins_shouldSkipAdminsWithNoEmail() {
        var event = event(1L, "Test");
        var admins = List.of(
                admin("", "No Email Admin"),
                admin("good@test.com", "Good Admin")
        );

        when(mailer.send(any(Mail[].class))).thenReturn(Uni.createFrom().voidItem());

        notificationService.sendToAdmins(event, admins).await().indefinitely();

        var captor = ArgumentCaptor.forClass(Mail[].class);
        verify(mailer).send(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue()[0].getTo()).contains("good@test.com");
    }

    @Test
    void sendToAdmins_shouldNotSendEmailWhenAdminListIsEmpty() {
        notificationService.sendToAdmins(event(1L, "Test"), List.of()).await().indefinitely();

        verify(mailer, never()).send(any(Mail[].class));
    }

    @Test
    void sendToAdmins_shouldNotSendEmailWhenAllAdminsHaveNoEmail() {
        var admins = List.of(admin("", "Admin One"), admin("  ", "Admin Two"));

        notificationService.sendToAdmins(event(1L, "Test"), admins).await().indefinitely();

        verify(mailer, never()).send(any(Mail[].class));
    }
}