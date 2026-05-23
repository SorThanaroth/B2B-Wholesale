package com.wholesale.marketplace.modules.report;

import com.wholesale.marketplace.modules.auth.User;
import com.wholesale.marketplace.modules.report.dto.AdminDashboardDto;
import com.wholesale.marketplace.modules.report.dto.MerchantDashboardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Section 9.11 — dashboards. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DashboardController {

    private final ReportService reportService;

    @GetMapping("/users/me/dashboard")
    public MerchantDashboardDto merchantDashboard(@AuthenticationPrincipal User user) {
        return reportService.merchantDashboard(user.getId());
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboardDto adminDashboard() {
        return reportService.adminDashboard();
    }
}
