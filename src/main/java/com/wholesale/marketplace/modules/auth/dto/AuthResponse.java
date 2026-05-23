package com.wholesale.marketplace.modules.auth.dto;

import com.wholesale.marketplace.modules.auth.Role;

import java.util.UUID;

/** Returned by login / refresh: the token pair plus a minimal user snapshot. */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String fullName,
        String email,
        Role role,
        /** Set only for SUPPLIER accounts — the company they represent. */
        UUID companyId
) {}
