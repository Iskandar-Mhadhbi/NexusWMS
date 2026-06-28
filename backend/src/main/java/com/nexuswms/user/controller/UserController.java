package com.nexuswms.user.controller;

import com.nexuswms.user.dto.UserResponse;
import com.nexuswms.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for handling user identity and profile operations.
 */
@RestController
@RequestMapping("/api/v{version}/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retrieves the profile information for the currently authenticated user.
     * * @param principalUserId The unique identifier extracted from the JWT token.
     * @return The UserResponse containing profile details wrapped in a 200 OK status.
     */
    @GetMapping(value = "/me", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal String principalUserId) {
        return ResponseEntity.ok(
                userService.getCurrentUser(principalUserId)
        );
    }
}