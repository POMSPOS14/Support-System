package com.company.support.user.repository;

import com.company.support.user.entity.User;
import com.company.support.user.entity.UserRole;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {
    // ReactivePanacheRepository уже возвращает Uni/Uni<List>

    public Uni<User> findByKeycloakId(String keycloakId) {
        return find("keycloakId", keycloakId).firstResult();
    }

    public Uni<User> findByLogin(String login) {
        return find("userLogin", login).firstResult();
    }

    public Uni<List<User>> findByRole(UserRole role) {
        return list("role", role);
    }
}
