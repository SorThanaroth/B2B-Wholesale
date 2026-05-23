package com.wholesale.marketplace.modules.auth.dto;

import com.wholesale.marketplace.modules.auth.Role;
import com.wholesale.marketplace.modules.auth.UserStatus;

/**
 * Result of self-registration. Self-registered accounts are created PENDING and
 * cannot sign in until an admin approves them, so no tokens are issued here.
 */
public record RegistrationResponse(
        String message,
        Role role,
        UserStatus status
) {}
