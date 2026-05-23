package com.wholesale.marketplace.modules.report.dto;

import java.math.BigDecimal;

/** Section 9.11 — platform-wide admin dashboard stats. */
public record AdminDashboardDto(
        BigDecimal totalRevenue,
        long totalOrders,
        long paidOrders,
        long activeMerchants,
        long activeCompanies,
        long pendingSettlements
) {}
