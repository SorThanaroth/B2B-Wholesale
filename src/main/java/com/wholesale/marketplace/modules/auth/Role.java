package com.wholesale.marketplace.modules.auth;

/**
 * Platform roles. Merchants buy at wholesale; suppliers manage their own
 * company's catalog/orders/settlements; admins manage the whole platform.
 */
public enum Role {
    MERCHANT,
    SUPPLIER,
    ADMIN
}
