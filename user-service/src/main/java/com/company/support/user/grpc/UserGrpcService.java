package com.company.support.user.grpc;

import com.company.support.user.entity.UserRole;
import com.company.support.user.mapper.UserMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class UserGrpcService extends MutinyUserServiceGrpc.UserServiceImplBase {

    @Inject
    com.company.support.user.service.UserService userService;

    @Inject
    UserMapper userMapper;

    @Override
    public Uni<UserResponse> getUserById(GetUserByIdRequest request) {
        return userService.findEntityById(request.getId())
                .map(userMapper::toProto);
    }

    @Override
    public Uni<UserListResponse> getUsersByRole(GetUsersByRoleRequest request) {
        UserRole role = UserRole.valueOf(request.getRole().toUpperCase());
        return userService.findEntitiesByRole(role)
                .map(users -> UserListResponse.newBuilder()
                        .addAllUsers(users.stream().map(userMapper::toProto).toList())
                        .build());
    }

    @Override
    public Uni<UserResponse> getUserByKeycloakId(GetUserByKeycloakIdRequest request) {
        return userService.findEntityByKeycloakId(request.getKeycloakId())
                .map(userMapper::toProto);
    }
}