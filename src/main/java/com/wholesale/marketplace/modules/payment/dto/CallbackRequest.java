package com.wholesale.marketplace.modules.payment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Webhook body from the (mock) gateway. {@code reference} is the order's qr_token;
 * {@code status} is PAID or FAILED. In production this would be signature-verified.
 */
public record CallbackRequest(
        @NotBlank String reference,
        @NotBlank String status
) {}
