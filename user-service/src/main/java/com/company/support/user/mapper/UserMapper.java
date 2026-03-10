package com.company.support.user.mapper;

import com.company.support.user.dto.request.CreateUserRequest;
import com.company.support.user.dto.request.UpdateUserRequest;
import com.company.support.user.dto.response.UserResponse;
import com.company.support.user.entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface UserMapper {

    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    User toEntity(CreateUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "userLogin", ignore = true)
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);

    default com.company.support.user.grpc.UserResponse toProto(User user) {
        return com.company.support.user.grpc.UserResponse.newBuilder()
                .setId(user.getId())
                .setRole(user.getRole().name())
                .setUserFullName(user.getUserFullName())
                .setUserLogin(user.getUserLogin())
                .setPhoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : "")
                .setEmail(user.getEmail())
                .setWorkplace(user.getWorkplace() != null ? user.getWorkplace() : "")
                .build();
    }
}