package com.wholesale.marketplace.modules.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MerchantActivityRow(UUID userId, String fullName, long orderCount, BigDecimal totalSpent) {}
