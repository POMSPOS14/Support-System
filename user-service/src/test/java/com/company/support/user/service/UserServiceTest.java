package com.company.support.user.service;

import com.company.support.user.dto.request.CreateUserRequest;
import com.company.support.user.dto.request.UpdateUserRequest;
import com.company.support.user.dto.response.UserResponse;
import com.company.support.user.entity.User;
import com.company.support.user.entity.UserRole;
import com.company.support.user.mapper.UserMapper;
import com.company.support.user.repository.UserRepository;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    // --- create ---

    @Test
    void create_shouldPersistUserWithKeycloakId() {
        var request = new CreateUserRequest();
        request.setUserLogin("testuser");
        request.setEmail("test@test.com");
        request.setUserFullName("Test User");
        request.setRole(UserRole.USER);
        request.setPassword("password123");

        var user = User.builder().id(1L).userLogin("testuser").keycloakId("kc-uuid-123").role(UserRole.USER).build();
        var response = UserResponse.builder().id(1L).userLogin("testuser").role(UserRole.USER).build();

        when(keycloakService.createUser(request)).thenReturn(Uni.createFrom().item("kc-uuid-123"));
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.persistAndFlush(any(User.class))).thenReturn(Uni.createFrom().item(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        var result = userService.create(request).await().indefinitely();

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserLogin()).isEqualTo("testuser");
        verify(keycloakService).createUser(request);
        verify(userRepository).persistAndFlush(any(User.class));
    }

    @Test
    void create_shouldSetKeycloakIdOnUser() {
        var request = new CreateUserRequest();
        request.setUserLogin("ivan");
        request.setEmail("ivan@test.com");
        request.setUserFullName("Ivan");
        request.setRole(UserRole.ADMIN);
        request.setPassword("pass");

        var user = User.builder().id(1L).build();
        when(keycloakService.createUser(request)).thenReturn(Uni.createFrom().item("kc-999"));
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.persistAndFlush(any(User.class))).thenReturn(Uni.createFrom().item(user));
        when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().id(1L).build());

        userService.create(request).await().indefinitely();

        assertThat(user.getKeycloakId()).isEqualTo("kc-999");
    }

    // --- findById ---

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
    void findById_shouldThrowNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Uni.createFrom().nullItem());

        assertThatThrownBy(() -> userService.findById(999L).await().indefinitely())
                .isInstanceOf(NotFoundException.class);
    }

    // --- findAll ---

    @Test
    void findAll_shouldReturnAllUsers() {
        var users = List.of(
                User.builder().id(1L).userLogin("user1").build(),
                User.builder().id(2L).userLogin("user2").build()
        );
        var responses = List.of(
                UserResponse.builder().id(1L).userLogin("user1").build(),
                UserResponse.builder().id(2L).userLogin("user2").build()
        );

        when(userRepository.listAll()).thenReturn(Uni.createFrom().item(users));
        when(userMapper.toResponseList(users)).thenReturn(responses);

        var result = userService.findAll().await().indefinitely();

        assertThat(result).hasSize(2);
    }

    // --- findByRole ---

    @Test
    void findByRole_shouldReturnUsersWithGivenRole() {
        var users = List.of(User.builder().id(1L).role(UserRole.ANALYST).build());
        var responses = List.of(UserResponse.builder().id(1L).role(UserRole.ANALYST).build());

        when(userRepository.findByRole(UserRole.ANALYST)).thenReturn(Uni.createFrom().item(users));
        when(userMapper.toResponseList(users)).thenReturn(responses);

        var result = userService.findByRole(UserRole.ANALYST).await().indefinitely();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(UserRole.ANALYST);
    }

    // --- update ---

    @Test
    void update_shouldApplyChangesAndCallKeycloak() {
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
        verify(keycloakService).updateUser(eq("kc-uuid-123"), eq(request));
    }

    @Test
    void update_shouldThrowNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Uni.createFrom().nullItem());

        assertThatThrownBy(() ->
                userService.update(999L, new UpdateUserRequest()).await().indefinitely())
                .isInstanceOf(NotFoundException.class);
    }

    // --- delete ---

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
    void delete_shouldThrowNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Uni.createFrom().nullItem());

        assertThatThrownBy(() -> userService.delete(999L).await().indefinitely())
                .isInstanceOf(NotFoundException.class);
    }

    // --- findEntityByKeycloakId ---

    @Test
    void findEntityByKeycloakId_shouldReturnUser() {
        var user = User.builder().id(1L).keycloakId("kc-abc").build();

        when(userRepository.findByKeycloakId("kc-abc")).thenReturn(Uni.createFrom().item(user));

        var result = userService.findEntityByKeycloakId("kc-abc").await().indefinitely();

        assertThat(result.getKeycloakId()).isEqualTo("kc-abc");
    }

    @Test
    void findEntityByKeycloakId_shouldThrowNotFoundWhenNotExists() {
        when(userRepository.findByKeycloakId("unknown")).thenReturn(Uni.createFrom().nullItem());

        assertThatThrownBy(() -> userService.findEntityByKeycloakId("unknown").await().indefinitely())
                .isInstanceOf(NotFoundException.class);
    }
}