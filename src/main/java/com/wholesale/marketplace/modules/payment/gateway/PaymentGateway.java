package com.wholesale.marketplace.modules.payment.gateway;

import java.math.BigDecimal;

/**
 * Abstraction over the unified-QR payment provider. The MVP ships a mock
 * ({@link MockKhqrGateway}); a real KHQR/Bakong adapter can implement this
 * interface without touching the rest of the system.
 */
public interface PaymentGateway {

    /** A generated charge: the EMVCo QR string to render and the provider's reference. */
    record GatewayCharge(String qrPayload, String gatewayRef) {}

    /**
     * Create a dynamic QR charge for {@code amount}, tagged with our {@code reference}
     * (the order's qr_token) so the eventual webhook can be correlated back.
     */
    GatewayCharge createCharge(BigDecimal amount, String reference);
}
