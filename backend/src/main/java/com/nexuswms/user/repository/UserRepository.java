package com.nexuswms.user.repository;

import com.nexuswms.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID; 

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    Optional<User> findByEmployeeId(String employeeId);
    Optional<User> findByGoogleId(String googleId);

    boolean existsByEmail(String email);
    boolean existsByEmployeeId(String employeeId);
    List<User> findAllByIdIn(Collection<UUID> ids);
    List<User> findAllByOrderByCreatedAtDesc();
}