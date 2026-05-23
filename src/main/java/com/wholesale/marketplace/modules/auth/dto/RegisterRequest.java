package com.wholesale.marketplace.modules.auth.dto;

import com.wholesale.marketplace.modules.auth.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-registration. {@code role} selects MERCHANT (buyer) or SUPPLIER (seller);
 * null defaults to MERCHANT. When SUPPLIER, the registrant also creates their
 * company (supplier = company), so {@code companyName}/{@code bankAccount} are
 * required — validated in {@code AuthService} since it's conditional.
 */
public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        String phone,

        /** MERCHANT or SUPPLIER. Null/ADMIN treated as MERCHANT-only at the service. */
        Role role,

        // ----- supplier-only (creates the company) -----
        String companyName,
        String bankAccount,
        String contactEmail
) {}
