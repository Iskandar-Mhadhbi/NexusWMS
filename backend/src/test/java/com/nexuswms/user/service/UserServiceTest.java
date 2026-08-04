package com.nexuswms.user.service;

import com.nexuswms.auth.service.TokenBlocklistService;
import com.nexuswms.common.exception.ConflictException;
import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.config.AppProperties;
import com.nexuswms.user.dto.request.UpdateRoleRequest;
import com.nexuswms.user.dto.request.UpdateStatusRequest;
import com.nexuswms.user.dto.response.UserResponse;
import com.nexuswms.user.dto.response.UserSummaryResponse;
import com.nexuswms.user.entity.Role;
import com.nexuswms.user.entity.User;
import com.nexuswms.user.entity.UserStatus;
import com.nexuswms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any; 
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService. All dependencies are mocked — no database,
 * no Spring context. Each test follows Arrange/Act/Assert: program the
 * mocks, call the real service method, assert on the result and/or verify
 * mock interactions.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenBlocklistService tokenBlocklistService;

    @Mock
    private AppProperties appProperties;

    private UserService userService;

    /** Reusable test fixture — a plain ACTIVE user, rebuilt fresh before every test. */
    private User activeUser;

    @BeforeEach
    void setUp() {
        // Manual construction instead of @InjectMocks: AppProperties is a
        // real (non-mocked-field) object with a mocked Jwt nested class
        // needed only by updateStatus() — @InjectMocks would still work
        // here since AppProperties itself is a @Mock, but manual wiring
        // keeps the constructor call explicit and easy to trace.
        userService = new UserService(userRepository, tokenBlocklistService, appProperties);
        AppProperties.Jwt jwt = mock(AppProperties.Jwt.class);
        lenient().when(appProperties.getJwt()).thenReturn(jwt);
        lenient().when(jwt.getExpirationMs()).thenReturn(604_800_000L);
        activeUser = User.builder()
                .id(UUID.randomUUID())
                .employeeId("EMP-0001")
                .email("test@nexuswms.com")
                .name("Test User")
                .role(Role.PICKER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
 
    @Nested
    class GetCurrentUser {

        @Test
        void returnsUserResponse_whenUserExists() {
            when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

            UserResponse result = userService.getCurrentUser(activeUser.getId().toString());

            assertThat(result.employeeId()).isEqualTo("EMP-0001");
            assertThat(result.email()).isEqualTo("test@nexuswms.com");
        }

        @Test
        void throwsResourceNotFound_whenUserDoesNotExist() {
            UUID missingId = UUID.randomUUID();
            when(userRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getCurrentUser(missingId.toString()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class GetAllUsers {

        @Test
        void returnsAllUsers_mappedToResponse() {
            when(userRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(activeUser));

            List<UserResponse> result = userService.getAllUsers();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).employeeId()).isEqualTo("EMP-0001");
        }

        @Test
        void returnsEmptyList_whenNoUsersExist() {
            when(userRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

            List<UserResponse> result = userService.getAllUsers();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class UpdateStatus {

        @Test
        void throwsResourceNotFound_whenEmployeeIdDoesNotExist() {
            when(userRepository.findByEmployeeId("EMP-9999")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userService.updateStatus("EMP-9999", new UpdateStatusRequest(UserStatus.INACTIVE)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void throwsConflict_whenStatusIsUnchanged() {
            when(userRepository.findByEmployeeId("EMP-0001")).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() ->
                    userService.updateStatus("EMP-0001", new UpdateStatusRequest(UserStatus.ACTIVE)))
                    .isInstanceOf(ConflictException.class);

            // Confirms the method fails fast before touching the blocklist or saving
            verifyNoInteractions(tokenBlocklistService);
            verify(userRepository, never()).save(any());
        }

        @Test
        void blocksUser_whenTransitioningToTerminated() {
            when(userRepository.findByEmployeeId("EMP-0001")).thenReturn(Optional.of(activeUser));
            AppProperties.Jwt jwt = mock(AppProperties.Jwt.class);
            when(appProperties.getJwt()).thenReturn(jwt);
            when(jwt.getExpirationMs()).thenReturn(604_800_000L); // 7 days, matches Phase 1 doc

            userService.updateStatus("EMP-0001", new UpdateStatusRequest(UserStatus.TERMINATED));

            verify(tokenBlocklistService).blockUser(
                    eq(activeUser.getId()), eq(Duration.ofMillis(604_800_000L)), eq("TERMINATED"));
            verify(userRepository).save(activeUser);
        }

        @Test
        void unblocksUser_whenTransitioningToActive() {
            User inactiveUser = User.builder()
                    .id(activeUser.getId())
                    .employeeId("EMP-0001")
                    .email("test@nexuswms.com")
                    .name("Test User")
                    .role(Role.PICKER)
                    .status(UserStatus.INACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(userRepository.findByEmployeeId("EMP-0001")).thenReturn(Optional.of(inactiveUser));

            userService.updateStatus("EMP-0001", new UpdateStatusRequest(UserStatus.ACTIVE));

            verify(tokenBlocklistService).unblockUser(inactiveUser.getId());
            verify(tokenBlocklistService, never()).blockUser(any(), any(), any());
        }

        @Test
        void doesNothingToBlocklist_whenTransitioningToPending() {
            User terminatedUser = User.builder()
                    .id(activeUser.getId())
                    .employeeId("EMP-0001")
                    .email("test@nexuswms.com")
                    .name("Test User")
                    .role(Role.PICKER)
                    .status(UserStatus.TERMINATED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(userRepository.findByEmployeeId("EMP-0001")).thenReturn(Optional.of(terminatedUser));

            userService.updateStatus("EMP-0001", new UpdateStatusRequest(UserStatus.PENDING));

            verifyNoInteractions(tokenBlocklistService);
            verify(userRepository).save(terminatedUser);
        }
    }

    @Nested
    class UpdateRole {

        @Test
        void updatesRole_andSaves() {
            when(userRepository.findByEmployeeId("EMP-0001")).thenReturn(Optional.of(activeUser));

            UserResponse result = userService.updateRole("EMP-0001", new UpdateRoleRequest(Role.MANAGER));

            assertThat(result.role()).isEqualTo(Role.MANAGER.name());
            verify(userRepository).save(activeUser);
        }

        @Test
        void throwsResourceNotFound_whenEmployeeIdDoesNotExist() {
            when(userRepository.findByEmployeeId("EMP-9999")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userService.updateRole("EMP-9999", new UpdateRoleRequest(Role.MANAGER)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class GetUserSummaries {

        @Test
        void returnsEmptyMap_whenIdSetIsNull() {
            Map<UUID, UserSummaryResponse> result = userService.getUserSummaries(null);

            assertThat(result).isEmpty();
            verifyNoInteractions(userRepository);
        }

        @Test
        void returnsEmptyMap_whenIdSetIsEmpty() {
            Map<UUID, UserSummaryResponse> result = userService.getUserSummaries(Set.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(userRepository);
        }

        @Test
        void returnsMapKeyedById_forMatchingUsers() {
            when(userRepository.findAllByIdIn(Set.of(activeUser.getId()))).thenReturn(List.of(activeUser));

            Map<UUID, UserSummaryResponse> result = userService.getUserSummaries(Set.of(activeUser.getId()));

            assertThat(result).hasSize(1);
            assertThat(result.get(activeUser.getId()).employeeId()).isEqualTo("EMP-0001");
        }

        @Test
        void silentlyOmitsIds_thatDoNotMatchAnyUser() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findAllByIdIn(Set.of(unknownId))).thenReturn(List.of());

            Map<UUID, UserSummaryResponse> result = userService.getUserSummaries(Set.of(unknownId));

            assertThat(result).isEmpty();
        }
    }
}