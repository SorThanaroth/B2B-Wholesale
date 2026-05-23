package com.wholesale.marketplace.modules.payment;

/** Lifecycle of a payment attempt (and, mirrored, an order's payment_status). */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    EXPIRED
}
