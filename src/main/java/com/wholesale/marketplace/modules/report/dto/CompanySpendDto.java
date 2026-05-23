package com.wholesale.marketplace.modules.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CompanySpendDto(UUID companyId, String companyName, BigDecimal amount, long orderCount) {}
