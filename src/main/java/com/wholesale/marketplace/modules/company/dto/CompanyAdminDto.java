package com.wholesale.marketplace.modules.company.dto;

import com.wholesale.marketplace.modules.company.Company;
import com.wholesale.marketplace.modules.company.CompanyStatus;

import java.time.Instant;
import java.util.UUID;

/** Admin company view — includes the settlement bank account. */
public record CompanyAdminDto(
        UUID id,
        String name,
        String logoUrl,
        String bankAccount,
        String contactEmail,
        CompanyStatus status,
        Instant createdAt
) {
    public static CompanyAdminDto from(Company c) {
        return new CompanyAdminDto(c.getId(), c.getName(), c.getLogoUrl(), c.getBankAccount(),
                c.getContactEmail(), c.getStatus(), c.getCreatedAt());
    }
}
