package com.wholesale.marketplace.modules.company.dto;

import com.wholesale.marketplace.modules.company.Company;
import com.wholesale.marketplace.modules.company.CompanyStatus;

import java.time.Instant;
import java.util.UUID;

/** Merchant-facing company view. Deliberately omits the settlement bank account. */
public record CompanyDto(
        UUID id,
        String name,
        String logoUrl,
        String contactEmail,
        CompanyStatus status,
        Instant createdAt
) {
    public static CompanyDto from(Company c) {
        return new CompanyDto(c.getId(), c.getName(), c.getLogoUrl(), c.getContactEmail(),
                c.getStatus(), c.getCreatedAt());
    }
}
