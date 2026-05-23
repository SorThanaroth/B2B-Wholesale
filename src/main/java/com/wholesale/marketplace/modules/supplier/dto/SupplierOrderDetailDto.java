package com.wholesale.marketplace.modules.supplier.dto;

import com.wholesale.marketplace.modules.order.OrderStatus;
import com.wholesale.marketplace.modules.order.SplitStatus;
import com.wholesale.marketplace.modules.order.dto.OrderItemDto;
import com.wholesale.marketplace.modules.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** An order detail scoped to the supplier: only their own line items + their split. */
public record SupplierOrderDetailDto(
        UUID orderId,
        Instant createdAt,
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        BigDecimal companySubtotal,
        SplitStatus settlementStatus,
        Instant paidAt,
        Instant settledAt,
        List<OrderItemDto> items
) {}
