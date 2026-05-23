package com.wholesale.marketplace.modules.payment.dto;

import com.wholesale.marketplace.modules.payment.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

/** The unified QR for an order: the EMVCo payload plus a ready-to-display PNG data URI. */
public record QrResponse(
        UUID orderId,
        String qrToken,
        String qrPayload,
        String qrImageDataUri,
        BigDecimal amount,
        PaymentStatus status
) {}
