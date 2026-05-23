package com.wholesale.marketplace.modules.supplier.dto;

import java.math.BigDecimal;
import java.util.List;

/** Supplier portal home stats — scoped to the supplier's own company. */
public record SupplierDashboardDto(
        long totalProducts,
        long activeProducts,
        long ordersInvolved,
        BigDecimal pendingSettlementAmount,
        BigDecimal settledAmount,
        List<SupplierOrderRowDto> recentOrders
) {}
