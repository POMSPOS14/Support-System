package com.company.support.user.service;

import com.company.support.user.dto.request.CreateUserRequest;
import com.company.support.user.dto.request.UpdateUserRequest;
import com.company.support.user.dto.response.UserResponse;
import com.company.support.user.entity.User;
import com.company.support.user.entity.UserRole;
import com.company.support.user.mapper.UserMapper;
import com.company.support.user.repository.UserRepository;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    KeycloakService keycloakService;

    @InjectMocks
    UserService userService;

    @Test
    void create_shouldPersistUserWithKeycloakId() {
        var request = new CreateUserRequest();
        request.setUserLogin("testuser");
        request.setEmail("test@test.com");
        request.setUserFullName("Test User");
        request.setRole(UserRole.USER);
        request.setPassword("password123");

        var user = User.builder()
                .id(1L)
                .userLogin("testuser")
                .email("test@test.com")
                .role(UserRole.USER)
                .keycloakId("kc-uuid-123")
                .build();

        var expectedResponse = UserResponse.builder()
                .id(1L)
                .userLogin("testuser")
                .role(UserRole.USER)
                .build();

        when(keycloakService.createUser(request)).thenReturn(Uni.createFrom().item("kc-uuid-123"));
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.persistAndFlush(any(User.class))).thenReturn(Uni.createFrom().item(user));
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        var result = userService.create(request).await().indefinitely();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserLogin()).isEqualTo("testuser");
        verify(keycloakService).createUser(request);
        verify(userRepository).persistAndFlush(any(User.class));
    }

    @Test
    void findById_shouldReturnUser() {
        var user = User.builder().id(1L).userLogin("testuser").role(UserRole.ADMIN).build();
        var response = UserResponse.builder().id(1L).userLogin("testuser").role(UserRole.ADMIN).build();

        when(userRepository.findById(1L)).thenReturn(Uni.createFrom().item(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        var result = userService.findById(1L).await().indefinitely();

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void delete_shouldCallKeycloakAndRepository() {
        var user = User.builder().id(1L).keycloakId("kc-uuid-123").build();

        when(userRepository.findById(1L)).thenReturn(Uni.createFrom().item(user));
        when(keycloakService.deleteUser("kc-uuid-123")).thenReturn(Uni.createFrom().voidItem());
        when(userRepository.delete(user)).thenReturn(Uni.createFrom().voidItem());

        userService.delete(1L).await().indefinitely();

        verify(keycloakService).deleteUser("kc-uuid-123");
        verify(userRepository).delete(user);
    }

    @Test
    void update_shouldApplyNonNullFieldsOnly() {
        var request = new UpdateUserRequest();
        request.setUserFullName("New Name");

        var user = User.builder().id(1L).keycloakId("kc-uuid-123").userFullName("Old Name").build();
        var response = UserResponse.builder().id(1L).userFullName("New Name").build();

        when(userRepository.findById(1L)).thenReturn(Uni.createFrom().item(user));
        when(keycloakService.updateUser(eq("kc-uuid-123"), eq(request))).thenReturn(Uni.createFrom().voidItem());
        when(userRepository.persistAndFlush(any(User.class))).thenReturn(Uni.createFrom().item(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        var result = userService.update(1L, request).await().indefinitely();

        assertThat(result.getUserFullName()).isEqualTo("New Name");
        verify(userMapper).updateEntity(eq(request), eq(user));
    }
}
