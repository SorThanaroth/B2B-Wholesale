package com.wholesale.marketplace.modules.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simulated KHQR/Bakong gateway. Builds a structurally-valid EMVCo dynamic-QR
 * payload (the same TLV format real KHQR uses, including a CRC-16/CCITT
 * checksum) so client QR renderers accept it — but no real money moves.
 * Payment confirmation is delivered by manually hitting the callback endpoint.
 */
@Component
@Slf4j
public class MockKhqrGateway implements PaymentGateway {

    private static final String MERCHANT_NAME = "B2B WHOLESALE";
    private static final String MERCHANT_CITY = "PHNOM PENH";
    private static final String CURRENCY_USD = "840";   // ISO 4217
    private static final String COUNTRY_KH = "KH";

    @Override
    public GatewayCharge createCharge(BigDecimal amount, String reference) {
        String gatewayRef = "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String payload = buildEmvCoPayload(amount, reference);
        log.info("[MOCK KHQR] Issued charge ref={} amount={} reference={}", gatewayRef, amount, reference);
        return new GatewayCharge(payload, gatewayRef);
    }

    private String buildEmvCoPayload(BigDecimal amount, String reference) {
        StringBuilder sb = new StringBuilder();
        sb.append(tlv("00", "01"));            // Payload Format Indicator
        sb.append(tlv("01", "12"));            // Point of Initiation Method: 12 = dynamic
        // Merchant Account Information (tag 30 — merchant) with a nested acquirer id + ref.
        String merchantAcct = tlv("00", "wholesale@b2b") + tlv("01", trim(reference, 25));
        sb.append(tlv("30", merchantAcct));
        sb.append(tlv("52", "5999"));          // Merchant Category Code (misc)
        sb.append(tlv("53", CURRENCY_USD));    // Transaction currency
        sb.append(tlv("54", amount.toPlainString())); // Transaction amount
        sb.append(tlv("58", COUNTRY_KH));      // Country code
        sb.append(tlv("59", MERCHANT_NAME));   // Merchant name
        sb.append(tlv("60", MERCHANT_CITY));   // Merchant city
        sb.append("6304");                      // CRC tag + length, value computed over everything so far
        sb.append(crc16(sb.toString()));
        return sb.toString();
    }

    /** EMVCo TLV: 2-digit tag, 2-digit zero-padded length, value. */
    private static String tlv(String tag, String value) {
        return tag + String.format("%02d", value.length()) + value;
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** CRC-16/CCITT-FALSE (poly 0x1021, init 0xFFFF) as used by EMVCo QR. */
    private static String crc16(String data) {
        int crc = 0xFFFF;
        for (byte b : data.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = ((crc & 0x8000) != 0) ? (crc << 1) ^ 0x1021 : crc << 1;
            }
        }
        return String.format("%04X", crc & 0xFFFF);
    }
}
