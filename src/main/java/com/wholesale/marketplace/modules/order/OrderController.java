package com.wholesale.marketplace.modules.order;

import com.wholesale.marketplace.common.dto.PageResponse;
import com.wholesale.marketplace.modules.auth.User;
import com.wholesale.marketplace.modules.order.dto.OrderDetailDto;
import com.wholesale.marketplace.modules.order.dto.OrderSummaryDto;
import com.wholesale.marketplace.modules.payment.dto.QrResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public QrResponse checkout(@AuthenticationPrincipal User user) {
        return orderService.checkout(user.getId());
    }

    @GetMapping
    public PageResponse<OrderSummaryDto> myOrders(@AuthenticationPrincipal User user,
                                                  @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return orderService.myOrders(user.getId(), pageable);
    }

    @GetMapping("/{id}")
    public OrderDetailDto getOrder(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return orderService.getMyOrder(user.getId(), id);
    }

    @PutMapping("/{orderId}/splits/{splitId}/confirm-delivery")
    public OrderDetailDto confirmDelivery(@AuthenticationPrincipal User user,
                                          @PathVariable UUID orderId,
                                          @PathVariable UUID splitId) {
        return orderService.confirmDelivery(user.getId(), orderId, splitId);
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> invoice(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        byte[] pdf = orderService.invoicePdf(user.getId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + id + ".pdf")
                .body(pdf);
    }
}
