package com.wholesale.marketplace.modules.order;

/** Fulfillment lifecycle shown in order history (Section 7.1). */
public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
