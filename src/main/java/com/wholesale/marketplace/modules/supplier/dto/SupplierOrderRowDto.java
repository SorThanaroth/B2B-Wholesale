package com.wholesale.marketplace.modules.supplier.dto;

import com.wholesale.marketplace.modules.order.FulfillmentStatus;
import com.wholesale.marketplace.modules.order.OrderStatus;
import com.wholesale.marketplace.modules.order.SplitStatus;
import com.wholesale.marketplace.modules.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One order the supplier's company is part of, with the company's own share. */
public record SupplierOrderRowDto(
        UUID orderId,
        Instant createdAt,
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        BigDecimal companySubtotal,
        SplitStatus settlementStatus,
        FulfillmentStatus fulfillmentStatus,
        UUID splitId,
        Instant paidAt
) {}
