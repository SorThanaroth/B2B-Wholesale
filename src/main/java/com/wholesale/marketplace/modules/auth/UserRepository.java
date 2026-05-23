package com.wholesale.marketplace.modules.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByRoleAndFullNameContainingIgnoreCase(Role role, String fullName, Pageable pageable);

    long countByRole(Role role);

    long countByRoleAndStatus(Role role, UserStatus status);
}
