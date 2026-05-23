package com.wholesale.marketplace.modules.order;

import com.wholesale.marketplace.common.dto.PageResponse;
import com.wholesale.marketplace.modules.order.dto.OrderDetailDto;
import com.wholesale.marketplace.modules.order.dto.OrderSummaryDto;
import com.wholesale.marketplace.modules.order.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/** Section 9.7 — Admin order management. */
@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public PageResponse<OrderSummaryDto> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID company,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return orderService.adminList(status, company, from, to, pageable);
    }

    @GetMapping("/{id}")
    public OrderDetailDto get(@PathVariable UUID id) {
        return orderService.adminGet(id);
    }

    @PutMapping("/{id}/status")
    public OrderSummaryDto updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateOrderStatusRequest req) {
        return orderService.updateStatus(id, req.status());
    }
}
