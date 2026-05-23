package com.wholesale.marketplace.modules.report;

import com.wholesale.marketplace.modules.report.dto.MerchantActivityRow;
import com.wholesale.marketplace.modules.report.dto.RevenueRow;
import com.wholesale.marketplace.modules.report.dto.TopProductRow;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/** Section 9.11 — admin analytics reports. */
@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/revenue")
    public List<RevenueRow> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return reportService.revenueByCompany(from, to);
    }

    @GetMapping("/products")
    public List<TopProductRow> topProducts(@RequestParam(defaultValue = "10") int limit) {
        return reportService.topProducts(limit);
    }

    @GetMapping("/merchants")
    public List<MerchantActivityRow> merchants(@RequestParam(defaultValue = "10") int limit) {
        return reportService.merchantActivity(limit);
    }
}
