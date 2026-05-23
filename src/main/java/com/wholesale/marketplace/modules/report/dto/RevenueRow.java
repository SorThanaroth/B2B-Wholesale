package com.wholesale.marketplace.modules.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RevenueRow(UUID companyId, String companyName, BigDecimal revenue, long orderCount) {}
