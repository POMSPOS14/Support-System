package com.company.support.user.service;

import com.company.support.user.dto.request.CreateUserRequest;
import com.company.support.user.dto.request.UpdateUserRequest;
import com.company.support.user.dto.response.UserResponse;
import com.company.support.user.entity.User;
import com.company.support.user.entity.UserRole;
import com.company.support.user.mapper.UserMapper;
import com.company.support.user.repository.UserRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    @Inject
    UserMapper userMapper;

    @Inject
    KeycloakService keycloakService;

    @WithSession
    public Uni<List<UserResponse>> findAll() {
        return userRepository.listAll()
                .map(userMapper::toResponseList);
    }

    @WithSession
    public Uni<UserResponse> findById(Long id) {
        return userRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("User not found: " + id))
                .map(userMapper::toResponse);
    }

    @WithSession
    public Uni<List<UserResponse>> findByRole(UserRole role) {
        return userRepository.findByRole(role)
                .map(userMapper::toResponseList);
    }

    @WithTransaction
    public Uni<UserResponse> create(CreateUserRequest request) {
        return keycloakService.createUser(request)
                .flatMap(keycloakId -> {
                    Log.info(keycloakId);
                    var user = userMapper.toEntity(request);
                    user.setKeycloakId(keycloakId);
                    return userRepository.persistAndFlush(user);
                })
                .map(userMapper::toResponse);
    }

    @WithTransaction
    public Uni<UserResponse> update(Long id, UpdateUserRequest request) {
        return userRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("User not found: " + id))
                .flatMap(user -> {
                    userMapper.updateEntity(request, user);
                    return keycloakService.updateUser(user.getKeycloakId(), request)
                            .replaceWith(userRepository.persistAndFlush(user));
                })
                .map(userMapper::toResponse);
    }

    @WithTransaction
    public Uni<Void> delete(Long id) {
        return userRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("User not found: " + id))
                .flatMap(user -> keycloakService.deleteUser(user.getKeycloakId())
                        .replaceWith(userRepository.delete(user)));
    }

    @WithSession
    public Uni<User> findEntityByKeycloakId(String keycloakId) {
        Uni<User> userUni = userRepository.findByKeycloakId(keycloakId)
                .onItem().ifNull().failWith(() -> new NotFoundException("User not found by keycloakId: " + keycloakId));
        return userUni;
    }

    // Методы для gRPC — вызываются из incident-service и notification-service

    @WithSession
    public Uni<User> findEntityById(Long id) {
        return userRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("User not found: " + id));
    }

    @WithSession
    public Uni<List<User>> findEntitiesByRole(UserRole role) {
        return userRepository.findByRole(role);
    }
}