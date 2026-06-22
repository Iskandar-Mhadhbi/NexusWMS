package com.nexuswms.user.service;

import com.nexuswms.shared.exception.ResourceNotFoundException;
import com.nexuswms.user.dto.UserResponse;
import com.nexuswms.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}