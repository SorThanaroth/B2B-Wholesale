package com.wholesale.marketplace.modules.order.dto;

import com.wholesale.marketplace.modules.order.OrderStatus;
import com.wholesale.marketplace.modules.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDetailDto(
        UUID id,
        UUID userId,
        BigDecimal totalAmount,
        OrderStatus status,
        PaymentStatus paymentStatus,
        String qrToken,
        Instant createdAt,
        List<OrderItemDto> items,
        List<OrderSplitDto> splits
) {}
