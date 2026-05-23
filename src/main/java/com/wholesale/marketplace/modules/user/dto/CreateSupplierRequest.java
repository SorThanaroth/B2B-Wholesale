package com.wholesale.marketplace.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Admin-provisioned supplier account, bound to one company (Section 5.1 / §10 v1.3). */
public record CreateSupplierRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank @Email(message = "Email must be valid") String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        String phone,
        @NotNull(message = "companyId is required") UUID companyId
) {}
