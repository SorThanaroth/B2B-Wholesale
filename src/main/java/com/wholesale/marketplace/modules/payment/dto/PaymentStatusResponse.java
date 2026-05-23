package com.wholesale.marketplace.modules.payment.dto;

import com.wholesale.marketplace.modules.order.OrderStatus;
import com.wholesale.marketplace.modules.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Polled by the SPA after checkout (Section 7.1 real-time payment status polling). */
public record PaymentStatusResponse(
        UUID orderId,
        PaymentStatus paymentStatus,
        OrderStatus orderStatus,
        BigDecimal amount,
        Instant paidAt
) {}
