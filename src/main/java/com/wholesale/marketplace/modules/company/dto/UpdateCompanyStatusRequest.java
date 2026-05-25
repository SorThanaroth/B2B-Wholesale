package com.wholesale.marketplace.modules.company.dto;

import com.wholesale.marketplace.modules.company.CompanyStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCompanyStatusRequest(
        @NotNull(message = "status is required") CompanyStatus status
) {}
