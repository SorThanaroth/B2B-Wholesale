package com.wholesale.marketplace.modules.user.dto;

import com.wholesale.marketplace.modules.auth.Role;
import com.wholesale.marketplace.modules.auth.User;
import com.wholesale.marketplace.modules.auth.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String fullName,
        String email,
        String phone,
        Role role,
        UserStatus status,
        boolean emailVerified,
        Instant createdAt,
        /** Populated for SUPPLIER accounts. */
        UUID companyId,
        String companyName
) {
    public static UserProfileDto from(User u) {
        return from(u, null);
    }

    /** Variant that includes the resolved company name (suppliers). */
    public static UserProfileDto from(User u, String companyName) {
        return new UserProfileDto(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getRole(), u.getStatus(), u.isEmailVerified(), u.getCreatedAt(),
                u.getCompanyId(), companyName);
    }
}
