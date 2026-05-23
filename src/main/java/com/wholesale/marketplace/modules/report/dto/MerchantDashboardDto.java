package com.wholesale.marketplace.modules.report.dto;

import com.wholesale.marketplace.modules.order.dto.OrderSummaryDto;

import java.math.BigDecimal;
import java.util.List;

/** Section 9.11 — merchant dashboard stats. */
public record MerchantDashboardDto(
        long totalOrders,
        long pendingPayments,
        BigDecimal totalSpent,
        List<OrderSummaryDto> recentOrders,
        List<CompanySpendDto> spendingByCompany
) {}
