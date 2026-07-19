package com.nexuswms.user.controller;

import com.nexuswms.user.dto.request.UpdateRoleRequest;
import com.nexuswms.user.dto.request.UpdateStatusRequest;
import com.nexuswms.user.dto.response.UserResponse;
import com.nexuswms.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse; 
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user profile and administration operations.
 *
 * <p>Exposes endpoints for retrieving the current user's profile,
 * listing all users, and admin-level status and role management.
 * Status and role mutations are restricted to the ADMIN role only.</p>
 */
@RestController
@RequestMapping("/api/v${spring.mvc.apiversion.supported}/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and administration")
public class UserController {

    private final UserService userService;

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @param principalUserId UUID of the authenticated user extracted from the JWT.
     * @return 200 OK with the user's profile details.
     */
    @Operation(summary = "Get current user profile")
    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully") 
    @GetMapping( "/me" )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal String principalUserId
    ) {
        return ResponseEntity.ok(userService.getCurrentUser(principalUserId));
    }

    /**
     * Returns all registered users ordered by registration date descending.
     * Restricted to ADMIN and MANAGER roles.
     *
     * @return 200 OK with a list of all user profiles.
     */
    @Operation(summary = "List all users")
    @ApiResponse(responseCode = "200", description = "User list retrieved successfully") 
    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Updates the account status of a user identified by employee ID.
     * Suspending or terminating a user immediately revokes their active token
     * via the Redis blocklist. Re-activating removes them from the blocklist.
     * Restricted to ADMIN role only.
     *
     * @param employeeId Human-readable employee identifier (e.g. EMP-0042).
     * @param request    Payload containing the new status.
     * @return 200 OK with the updated user profile.
     */
    @Operation(summary = "Update user account status (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "Status updated successfully") 
    @PatchMapping( "/{employeeId}/status" )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable String employeeId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(userService.updateStatus(employeeId, request));
    }

    /**
     * Updates the assigned role of a user identified by employee ID.
     * The role change takes effect on the user's next login — the existing
     * JWT still carries the old role until it expires or the user logs out.
     * Restricted to ADMIN role only.
     *
     * @param employeeId Human-readable employee identifier (e.g. EMP-0042).
     * @param request    Payload containing the new role.
     * @return 200 OK with the updated user profile.
     */
    @Operation(summary = "Update user role (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "Role updated successfully") 
    @PatchMapping( "/{employeeId}/role" )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable String employeeId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(userService.updateRole(employeeId, request));
    }
}