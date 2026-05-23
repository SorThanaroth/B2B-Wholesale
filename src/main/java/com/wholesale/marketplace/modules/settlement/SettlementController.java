package com.wholesale.marketplace.modules.settlement;

import com.wholesale.marketplace.common.dto.PageResponse;
import com.wholesale.marketplace.modules.order.SplitStatus;
import com.wholesale.marketplace.modules.settlement.dto.SettlementDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Section 9.9 — Settlements (admin only). */
@RestController
@RequestMapping("/api/v1/admin/settlements")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public PageResponse<SettlementDto> list(
            @RequestParam(required = false) UUID company,
            @RequestParam(required = false) SplitStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return settlementService.list(company, status, from, to, pageable);
    }

    @GetMapping("/{orderId}")
    public List<SettlementDto> forOrder(@PathVariable UUID orderId) {
        return settlementService.forOrder(orderId);
    }

    @PutMapping("/{id}/settle")
    public SettlementDto settle(@PathVariable UUID id) {
        return settlementService.settle(id);
    }

    @GetMapping("/report")
    public ResponseEntity<byte[]> report(
            @RequestParam(required = false) UUID company,
            @RequestParam(required = false) SplitStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        byte[] csv = settlementService.reportCsv(company, status, from, to);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=settlement-report.csv")
                .body(csv);
    }
}
