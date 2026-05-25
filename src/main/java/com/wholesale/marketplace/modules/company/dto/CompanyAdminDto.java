package com.wholesale.marketplace.modules.company.dto;

import com.wholesale.marketplace.modules.company.Company;
import com.wholesale.marketplace.modules.company.CompanyStatus;

import java.time.Instant;
import java.util.UUID;

/** Admin company view — full detail incl. settlement bank account + KYC fields for review. */
public record CompanyAdminDto(
        UUID id,
        String name,
        String logoUrl,
        String bankAccount,
        String contactEmail,
        String registrationNo,
        String phone,
        String address,
        String description,
        CompanyStatus status,
        Instant createdAt
) {
    public static CompanyAdminDto from(Company c) {
        return new CompanyAdminDto(c.getId(), c.getName(), c.getLogoUrl(), c.getBankAccount(),
                c.getContactEmail(), c.getRegistrationNo(), c.getPhone(), c.getAddress(),
                c.getDescription(), c.getStatus(), c.getCreatedAt());
    }
}
