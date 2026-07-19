package com.nexuswms.auth.controller;

import com.nexuswms.auth.dto.request.LoginRequest;
import com.nexuswms.auth.dto.request.RegisterRequest;
import com.nexuswms.auth.dto.response.AuthResponse;
import com.nexuswms.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse; 
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication lifecycle operations.
 *
 * <p>Handles user registration, login, and logout. Registration and login
 * are publicly accessible. Logout requires a valid JWT — the token is
 * immediately invalidated via the Redis blocklist on successful logout.</p>
 */
@RestController
@RequestMapping("/api/v${spring.mvc.apiversion.supported}/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, and logout")
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user account.
     * New accounts are assigned PICKER role and PENDING status by default.
     * An admin must activate the account before the user can log in.
     *
     * @param request Registration payload containing employee ID, name, email, and password.
     * @return 201 Created with a JWT and the created user's details.
     */
    @Operation(summary = "Register a new user account")
    @ApiResponse(responseCode = "201", description = "Account created successfully") 
    @PostMapping( "/register" )
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    /**
     * Authenticates a user by email or employee ID and returns a JWT.
     * The identifier type is detected automatically — values containing '@'
     * are treated as email addresses, all others as employee IDs.
     * Only ACTIVE accounts can log in.
     *
     * @param request Login payload containing identifier and password.
     * @return 200 OK with a JWT and the user's details.
     */
    @Operation(summary = "Login with email or employee ID")
    @ApiResponse(responseCode = "200", description = "Login successful") 
    @PostMapping( "/login" )
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Logs out the current user by invalidating their JWT via the Redis blocklist.
     * The blocklist entry TTL matches the token's remaining lifetime,
     * ensuring automatic cleanup with zero maintenance overhead.
     *
     * @param authorizationHeader The Authorization header containing the Bearer token.
     * @param principalUserId     UUID of the authenticated user extracted from the JWT.
     * @return 204 No Content on successful logout.
     */
    @Operation(summary = "Logout and invalidate the current JWT")
    @ApiResponse(responseCode = "204", description = "Logged out successfully") 
    @PostMapping("/logout" )
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authorizationHeader,
            @AuthenticationPrincipal String principalUserId
    ) {
        String token = authorizationHeader.substring(7); // Strip "Bearer " prefix
        authService.logout(token, principalUserId);
        return ResponseEntity.noContent().build();
    }
}