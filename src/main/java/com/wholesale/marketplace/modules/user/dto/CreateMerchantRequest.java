package com.wholesale.marketplace.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin-created merchant account (active immediately). */
public record CreateMerchantRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank @Email(message = "Email must be valid") String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        String phone
) {}
