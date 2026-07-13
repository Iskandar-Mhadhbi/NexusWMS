package com.nexuswms.auth.service;

import com.nexuswms.auth.dto.request.LoginRequest;
import com.nexuswms.auth.dto.request.RegisterRequest;
import com.nexuswms.auth.dto.response.AuthResponse;
import com.nexuswms.auth.security.JwtUtil;
import com.nexuswms.common.exception.ConflictException;
import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.user.entity.Role;
import com.nexuswms.user.entity.User;
import com.nexuswms.user.entity.UserStatus;
import com.nexuswms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service responsible for user authentication lifecycle.
 *
 * <p>Handles registration, login, and logout. Logout immediately revokes
 * the user's token via the Redis blocklist rather than waiting for natural expiry.</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlocklistService tokenBlocklistService;

    /* ----- Registration ----- */

    /**
     * Registers a new user account with PICKER role and PENDING status.
     * An admin must activate the account before the user can log in.
     *
     * @param request Registration payload containing employee ID, name, email, and password.
     * @return AuthResponse containing a JWT and the created user's details.
     * @throws ConflictException if the email or employee ID is already registered.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered: " + request.email());
        }
        if (userRepository.existsByEmployeeId(request.employeeId())) {
            throw new ConflictException("Employee ID already registered: " + request.employeeId());
        }

        User user = User.builder()
                .employeeId(request.employeeId())
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.PICKER)
                .status(UserStatus.PENDING)
                .build();

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(
                user.getId(), user.getEmail(), user.getRole().name()
        );

        return new AuthResponse(
                token,
                user.getId().toString(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }

    /* ----- Login ----- */

    /**
     * Authenticates a user by email or employee ID.
     * Identifier type is detected automatically via the presence of '@'.
     * Only ACTIVE accounts can log in.
     *
     * @param request Login payload containing identifier and password.
     * @return AuthResponse containing a JWT and the user's details.
     * @throws BadCredentialsException   if the password is wrong or the account is not active.
     * @throws ResourceNotFoundException if no account matches the identifier.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = request.identifier().contains("@")
                ? userRepository.findByEmail(request.identifier())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No account found with email: " + request.identifier()))
                : userRepository.findByEmployeeId(request.identifier())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No account found with employee ID: " + request.identifier()));

        if (user.getPasswordHash() == null) {
            throw new BadCredentialsException(
                    "This account uses Google login. Please sign in with Google.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        // Status check deferred until after credential verification
        // to avoid leaking account existence via different error messages
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException(
                    "Account is not active. Current status: " + user.getStatus().name());
        }

        String token = jwtUtil.generateToken(
                user.getId(), user.getEmail(), user.getRole().name()
        );

        return new AuthResponse(
                token,
                user.getId().toString(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }

    /* ----- Logout ----- */

    /**
     * Invalidates the current user's JWT by adding it to the Redis blocklist.
     * TTL is set to the token's remaining lifetime so the entry auto-expires
     * exactly when the token would have expired naturally — zero maintenance required.
     *
     * @param token           Raw JWT string extracted from the Authorization header.
     * @param principalUserId UUID of the authenticated user from the JWT subject claim.
     */
    public void logout(String token, String principalUserId) {
        tokenBlocklistService.blockUser(
                UUID.fromString(principalUserId),
                jwtUtil.extractRemainingTtl(token),
                "LOGOUT"
        );
    }
}