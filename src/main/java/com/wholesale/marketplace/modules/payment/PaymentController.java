package com.wholesale.marketplace.modules.payment;

import com.wholesale.marketplace.modules.auth.User;
import com.wholesale.marketplace.modules.payment.dto.CallbackRequest;
import com.wholesale.marketplace.modules.payment.dto.PaymentStatusResponse;
import com.wholesale.marketplace.modules.payment.dto.QrResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** Section 9.8 — Payments. The callback is the (public) gateway webhook. */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{orderId}/qr")
    public QrResponse getQr(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        return paymentService.getQr(orderId, user);
    }

    @GetMapping("/{orderId}/status")
    public PaymentStatusResponse getStatus(@PathVariable UUID orderId, @AuthenticationPrincipal User user) {
        return paymentService.getStatus(orderId, user);
    }

    /**
     * Public webhook (KHQR/Bakong → us). In the MVP this is also how you
     * simulate a paid bill: POST {"reference":"<order qr_token>","status":"PAID"}.
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> callback(@Valid @RequestBody CallbackRequest req) {
        paymentService.handleCallback(req);
        return ResponseEntity.ok(Map.of("message", "Callback processed"));
    }
}
