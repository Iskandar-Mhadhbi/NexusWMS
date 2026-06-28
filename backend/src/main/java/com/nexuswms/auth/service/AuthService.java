package com.nexuswms.auth.service;

import com.nexuswms.auth.dto.AuthResponse;
import com.nexuswms.auth.dto.LoginRequest;
import com.nexuswms.auth.dto.RegisterRequest;
import com.nexuswms.auth.security.JwtUtil;
import com.nexuswms.common.exception.ConflictException;
import com.nexuswms.common.exception.ResourceNotFoundException;
import com.nexuswms.user.entity.Role;
import com.nexuswms.user.entity.User;
import com.nexuswms.user.entity.UserStatus;
import com.nexuswms.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered: " + request.email());
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

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getId().toString(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user;
        if (request.identifier().contains("@")) {
            user = userRepository.findByEmail(request.identifier())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No account found with email: " + request.identifier()));
        } else {
            user = userRepository.findByEmployeeId(request.identifier())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No account found with employee ID: " + request.identifier()));
        }
        if (user.getPasswordHash() == null) {
            throw new BadCredentialsException(
                "This account uses Google login. Please sign in with Google.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        //information about account must be returned only in case of correct credentials
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException(
                "Account is not active. Current status: " + user.getStatus().name()
            );
        }
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getId().toString(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }
}