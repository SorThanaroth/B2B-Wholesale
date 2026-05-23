package com.wholesale.marketplace.modules.order.dto;

import com.wholesale.marketplace.modules.order.Order;
import com.wholesale.marketplace.modules.order.OrderStatus;
import com.wholesale.marketplace.modules.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryDto(
        UUID id,
        BigDecimal totalAmount,
        OrderStatus status,
        PaymentStatus paymentStatus,
        Instant createdAt
) {
    public static OrderSummaryDto from(Order o) {
        return new OrderSummaryDto(o.getId(), o.getTotalAmount(), o.getStatus(),
                o.getPaymentStatus(), o.getCreatedAt());
    }
}
