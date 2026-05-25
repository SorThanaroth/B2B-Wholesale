package com.wholesale.marketplace.modules.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(
        @NotBlank(message = "Company name is required") String name,
        String logoUrl,
        @NotBlank(message = "Bank account is required for settlement") String bankAccount,
        @Email(message = "Contact email must be valid") String contactEmail,
        // KYC / profile detail for review
        String registrationNo,
        String phone,
        String address,
        String description
) {}
