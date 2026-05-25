package com.wholesale.marketplace.modules.order.dto;

import com.wholesale.marketplace.modules.order.FulfillmentStatus;
import com.wholesale.marketplace.modules.order.SplitStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSplitDto(
        UUID id,
        UUID companyId,
        String companyName,
        BigDecimal subtotal,
        SplitStatus paymentStatus,
        FulfillmentStatus fulfillmentStatus,
        Instant paidAt,
        Instant settledAt
) {}
