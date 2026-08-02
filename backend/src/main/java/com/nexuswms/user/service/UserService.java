package com.nexuswms.user.service;

import com.nexuswms.auth.service.TokenBlocklistService;
import com.nexuswms.common.exception.ConflictException;
import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.config.AppProperties;
import com.nexuswms.user.dto.request.UpdateRoleRequest;
import com.nexuswms.user.dto.request.UpdateStatusRequest;
import com.nexuswms.user.dto.response.UserResponse;
import com.nexuswms.user.dto.response.UserSummaryResponse;
import com.nexuswms.user.entity.User;
import com.nexuswms.user.entity.UserStatus;
import com.nexuswms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID; 
import java.util.stream.Collectors; 
/**
 * Service responsible for user profile and administration operations.
 *
 * <p>Status changes that suspend or terminate a user immediately invalidate
 * their active token via the Redis blocklist. Re-activating a user removes
 * them from the blocklist, allowing them to authenticate with a fresh token.</p>
 *
 * <p>Note: role changes take effect on the user's next login. The existing
 * JWT still carries the old role until it expires or the user logs out.</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TokenBlocklistService tokenBlocklistService;
    private final AppProperties appProperties;

    /* ----- Query ----- */

    /**
     * Retrieves the profile of the currently authenticated user by their UUID.
     *
     * @param userId The user's UUID as a string, extracted from the JWT subject claim.
     * @return UserResponse containing the user's profile details.
     * @throws ResourceNotFoundException if no user exists with the given ID.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    /**
     * Returns all registered users ordered by registration date descending.
     *
     * @return List of UserResponse for all users in the system.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    /* ----- Mutation ----- */

    /**
     * Updates the account status of a user identified by employee ID.
     * Suspending or terminating a user immediately adds them to the Redis blocklist,
     * revoking their active token for the duration of the maximum token lifetime.
     * Re-activating a user removes them from the blocklist.
     *
     * @param employeeId The human-readable employee identifier (e.g. EMP-0042).
     * @param request    Payload containing the new status.
     * @return Updated UserResponse.
     * @throws ResourceNotFoundException if no user exists with the given employee ID.
     * @throws ConflictException         if the user already has the requested status.
     */
    @Transactional
    public UserResponse updateStatus(String employeeId, UpdateStatusRequest request) {
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", employeeId));

        UserStatus newStatus = request.status();

        if (user.getStatus() == newStatus) {
            throw new ConflictException(
                    "User " + employeeId + " already has status: " + newStatus.name());
        }

        user.setStatus(newStatus);
        userRepository.save(user);

        // Use maximum token lifetime as TTL since we don't have the user's actual token here
        Duration blockTtl = Duration.ofMillis(appProperties.getJwt().getExpirationMs());

        switch (newStatus) {
            case INACTIVE, ON_LEAVE, TERMINATED ->
                    tokenBlocklistService.blockUser(user.getId(), blockTtl, newStatus.name());
            case ACTIVE ->
                    tokenBlocklistService.unblockUser(user.getId());
            default -> { /* PENDING — no Redis action needed */ }
        }

        return UserResponse.from(user);
    }

    /**
     * Updates the assigned role of a user identified by employee ID.
     * The role change takes effect on the user's next login — their existing
     * JWT still carries the old role until it expires naturally.
     *
     * @param employeeId The human-readable employee identifier (e.g. EMP-0042).
     * @param request    Payload containing the new role.
     * @return Updated UserResponse.
     * @throws ResourceNotFoundException if no user exists with the given employee ID.
     */
    @Transactional
    public UserResponse updateRole(String employeeId, UpdateRoleRequest request) {
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", employeeId));

        user.setRole(request.role());
        userRepository.save(user);

        return UserResponse.from(user);
    }

    
    /**
     * Returns user summaries for the given IDs in a single query.
     * IDs that don’t match any user are silently ignored.
     */
    public Map<UUID, UserSummaryResponse> getUserSummaries(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllByIdIn(ids).stream()
                .map(UserSummaryResponse::from)
                .collect(Collectors.toMap(r -> r.id(),r -> r ));
    }
}