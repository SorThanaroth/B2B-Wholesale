package com.wholesale.marketplace.modules.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Re-points a supplier account at a different company. */
public record AssignCompanyRequest(
        @NotNull(message = "companyId is required") UUID companyId
) {}
