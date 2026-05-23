package com.wholesale.marketplace.modules.auth;

/** Account lifecycle states. Admins approve/suspend merchant accounts. */
public enum UserStatus {
    PENDING,    // registered, awaiting approval (if approval flow is enabled)
    ACTIVE,     // may log in and transact
    SUSPENDED   // blocked by an admin
}
