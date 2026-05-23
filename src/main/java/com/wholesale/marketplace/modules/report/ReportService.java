package com.wholesale.marketplace.modules.report;

import com.wholesale.marketplace.modules.auth.Role;
import com.wholesale.marketplace.modules.auth.User;
import com.wholesale.marketplace.modules.auth.UserRepository;
import com.wholesale.marketplace.modules.auth.UserStatus;
import com.wholesale.marketplace.modules.company.Company;
import com.wholesale.marketplace.modules.company.CompanyRepository;
import com.wholesale.marketplace.modules.company.CompanyStatus;
import com.wholesale.marketplace.modules.order.OrderCompanySplitRepository;
import com.wholesale.marketplace.modules.order.OrderRepository;
import com.wholesale.marketplace.modules.order.SplitStatus;
import com.wholesale.marketplace.modules.order.dto.OrderSummaryDto;
import com.wholesale.marketplace.modules.payment.PaymentStatus;
import com.wholesale.marketplace.modules.report.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Section 9.11 — dashboard and analytics aggregations. */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final OrderRepository orderRepository;
    private final OrderCompanySplitRepository splitRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MerchantDashboardDto merchantDashboard(UUID userId) {
        long totalOrders = orderRepository.countByUserId(userId);
        long pendingPayments = orderRepository.countByUserIdAndPaymentStatus(userId, PaymentStatus.PENDING);
        BigDecimal totalSpent = zeroIfNull(reportRepository.sumPaidByUser(userId));

        List<OrderSummaryDto> recent = orderRepository
                .findByUserId(userId, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(OrderSummaryDto::from).getContent();

        List<CompanyAmount> spend = reportRepository.spendingByCompanyForUser(userId);
        Map<UUID, String> names = companyNames(spend.stream().map(CompanyAmount::companyId).collect(Collectors.toSet()));
        List<CompanySpendDto> spendingByCompany = spend.stream()
                .map(a -> new CompanySpendDto(a.companyId(), names.get(a.companyId()), a.revenue(), a.orderCount()))
                .toList();

        return new MerchantDashboardDto(totalOrders, pendingPayments, totalSpent, recent, spendingByCompany);
    }

    @Transactional(readOnly = true)
    public AdminDashboardDto adminDashboard() {
        return new AdminDashboardDto(
                zeroIfNull(reportRepository.sumTotalRevenue()),
                orderRepository.count(),
                orderRepository.countByPaymentStatus(PaymentStatus.PAID),
                userRepository.countByRoleAndStatus(Role.MERCHANT, UserStatus.ACTIVE),
                companyRepository.countByStatus(CompanyStatus.ACTIVE),
                splitRepository.countByPaymentStatus(SplitStatus.PENDING_SETTLEMENT));
    }

    @Transactional(readOnly = true)
    public List<RevenueRow> revenueByCompany(Instant from, Instant to) {
        Instant lo = from != null ? from : Instant.EPOCH;
        Instant hi = to != null ? to : Instant.now();
        List<CompanyAmount> rows = reportRepository.revenueByCompany(lo, hi);
        Map<UUID, String> names = companyNames(rows.stream().map(CompanyAmount::companyId).collect(Collectors.toSet()));
        return rows.stream()
                .map(a -> new RevenueRow(a.companyId(), names.get(a.companyId()), a.revenue(), a.orderCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopProductRow> topProducts(int limit) {
        return reportRepository.topProducts(PageRequest.of(0, Math.max(1, limit))).stream()
                .map(p -> new TopProductRow(p.productId(), p.productName(), p.quantitySold(), p.revenue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MerchantActivityRow> merchantActivity(int limit) {
        List<MerchantActivity> rows = reportRepository.merchantActivity(PageRequest.of(0, Math.max(1, limit)));
        Map<UUID, String> names = userRepository.findAllById(
                        rows.stream().map(MerchantActivity::userId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
        return rows.stream()
                .map(r -> new MerchantActivityRow(r.userId(), names.get(r.userId()), r.orderCount(), r.totalSpent()))
                .toList();
    }

    // ----- helpers -------------------------------------------------------

    private Map<UUID, String> companyNames(Set<UUID> ids) {
        return companyRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Company::getId, Company::getName));
    }

    private static BigDecimal zeroIfNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
