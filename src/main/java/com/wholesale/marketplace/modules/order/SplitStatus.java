package com.wholesale.marketplace.modules.order;

/**
 * Settlement state of one company's share of an order (Section 8.2):
 * PENDING (order unpaid) → PENDING_SETTLEMENT (merchant paid, awaiting payout) → SETTLED.
 */
public enum SplitStatus {
    PENDING,
    PENDING_SETTLEMENT,
    SETTLED
}
