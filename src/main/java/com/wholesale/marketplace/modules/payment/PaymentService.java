package com.wholesale.marketplace.modules.payment;

import com.wholesale.marketplace.common.exception.BadRequestException;
import com.wholesale.marketplace.common.exception.ResourceNotFoundException;
import com.wholesale.marketplace.modules.auth.Role;
import com.wholesale.marketplace.modules.auth.User;
import com.wholesale.marketplace.modules.order.*;
import com.wholesale.marketplace.modules.payment.dto.CallbackRequest;
import com.wholesale.marketplace.modules.payment.dto.PaymentStatusResponse;
import com.wholesale.marketplace.modules.payment.dto.QrResponse;
import com.wholesale.marketplace.modules.payment.gateway.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Generates the unified QR at checkout and handles the gateway webhook. On a
 * PAID callback it flips the order to PAID and each company split to
 * PENDING_SETTLEMENT — the moment the settlement obligations come into being
 * (Section 8, step 8).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderCompanySplitRepository splitRepository;
    private final PaymentGateway gateway;
    private final QrRenderer qrRenderer;

    /** Called by OrderService during checkout. Creates the Payment row + QR. */
    @Transactional
    public QrResponse createForOrder(Order order) {
        PaymentGateway.GatewayCharge charge = gateway.createCharge(order.getTotalAmount(), order.getQrToken());
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .gatewayRef(charge.gatewayRef())
                .qrCodeUrl("/api/v1/payments/" + order.getId() + "/qr")
                .build();
        paymentRepository.save(payment);
        return new QrResponse(order.getId(), order.getQrToken(), charge.qrPayload(),
                qrRenderer.toDataUri(charge.qrPayload()), order.getTotalAmount(), PaymentStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public QrResponse getQr(UUID orderId, User requester) {
        Order order = authorizeOrder(orderId, requester);
        // Deterministic re-render from (amount, qr_token).
        PaymentGateway.GatewayCharge charge = gateway.createCharge(order.getTotalAmount(), order.getQrToken());
        return new QrResponse(order.getId(), order.getQrToken(), charge.qrPayload(),
                qrRenderer.toDataUri(charge.qrPayload()), order.getTotalAmount(), order.getPaymentStatus());
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(UUID orderId, User requester) {
        Order order = authorizeOrder(orderId, requester);
        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId).orElse(null);
        return new PaymentStatusResponse(order.getId(), order.getPaymentStatus(), order.getStatus(),
                order.getTotalAmount(), payment == null ? null : payment.getPaidAt());
    }

    /**
     * Gateway webhook. Idempotent: a second PAID callback for an already-paid
     * order is a no-op. This is the trigger for the settlement split.
     */
    @Transactional
    public void handleCallback(CallbackRequest req) {
        Order order = orderRepository.findByQrToken(req.reference())
                .orElseThrow(() -> new BadRequestException("Unknown payment reference"));
        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
                .orElseThrow(() -> new BadRequestException("No payment found for order"));

        String status = req.status().trim().toUpperCase();
        switch (status) {
            case "PAID" -> markPaid(order, payment);
            case "FAILED" -> markFailed(order, payment);
            default -> throw new BadRequestException("Unsupported callback status: " + req.status());
        }
    }

    private void markPaid(Order order, Payment payment) {
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("[PAYMENT] Duplicate PAID callback for order {} ignored", order.getId());
            return;
        }
        Instant now = Instant.now();
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(now);
        paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        List<OrderCompanySplit> splits = splitRepository.findByOrderId(order.getId());
        for (OrderCompanySplit split : splits) {
            split.setPaymentStatus(SplitStatus.PENDING_SETTLEMENT);
            split.setPaidAt(now);
        }
        splitRepository.saveAll(splits);
        log.info("[PAYMENT] Order {} PAID; {} company split(s) -> PENDING_SETTLEMENT", order.getId(), splits.size());
    }

    private void markFailed(Order order, Payment payment) {
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        order.setPaymentStatus(PaymentStatus.FAILED);
        orderRepository.save(order);
        log.info("[PAYMENT] Order {} payment FAILED", order.getId());
    }

    private Order authorizeOrder(UUID orderId, User requester) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (requester.getRole() != Role.ADMIN && !order.getUserId().equals(requester.getId())) {
            throw new AccessDeniedException("This order does not belong to you");
        }
        return order;
    }
}
