package com.wholesale.marketplace.modules.settlement;

import com.wholesale.marketplace.common.dto.PageResponse;
import com.wholesale.marketplace.common.exception.BadRequestException;
import com.wholesale.marketplace.common.exception.ResourceNotFoundException;
import com.wholesale.marketplace.modules.company.Company;
import com.wholesale.marketplace.modules.company.CompanyRepository;
import com.wholesale.marketplace.modules.order.FulfillmentStatus;
import com.wholesale.marketplace.modules.order.OrderCompanySplit;
import com.wholesale.marketplace.modules.order.OrderCompanySplitRepository;
import com.wholesale.marketplace.modules.order.SplitStatus;
import com.wholesale.marketplace.modules.settlement.dto.SettlementDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin settlement operations over {@code order_company_splits}: review what is
 * owed per company and mark payouts as settled (Section 8.2, step 10).
 */
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final OrderCompanySplitRepository splitRepository;
    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public PageResponse<SettlementDto> list(UUID companyId, SplitStatus status,
                                            Instant from, Instant to, Pageable pageable) {
        Specification<OrderCompanySplit> spec = Specification.where(SettlementSpecifications.company(companyId))
                .and(SettlementSpecifications.status(status))
                .and(SettlementSpecifications.paidFrom(from))
                .and(SettlementSpecifications.paidTo(to));
        Page<OrderCompanySplit> page = splitRepository.findAll(spec, pageable);
        Map<UUID, Company> companies = loadCompanies(page.getContent());
        return new PageResponse<>(
                page.getContent().stream().map(s -> toDto(s, companies)).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public List<SettlementDto> forOrder(UUID orderId) {
        List<OrderCompanySplit> splits = splitRepository.findByOrderId(orderId);
        Map<UUID, Company> companies = loadCompanies(splits);
        return splits.stream().map(s -> toDto(s, companies)).toList();
    }

    @Transactional
    public SettlementDto settle(UUID splitId) {
        OrderCompanySplit split = splitRepository.findById(splitId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement split", splitId));
        if (split.getPaymentStatus() == SplitStatus.PENDING) {
            throw new BadRequestException("Cannot settle a split whose order has not been paid yet");
        }
        if (split.getPaymentStatus() == SplitStatus.SETTLED) {
            throw new BadRequestException("This split is already settled");
        }
        // Pay out to the supplier only once the goods have arrived (merchant-confirmed delivery).
        if (split.getFulfillmentStatus() != FulfillmentStatus.DELIVERED) {
            throw new BadRequestException("Can only settle once the order has arrived (delivery confirmed by the merchant)");
        }
        split.setPaymentStatus(SplitStatus.SETTLED);
        split.setSettledAt(Instant.now());
        OrderCompanySplit saved = splitRepository.save(split);
        return toDto(saved, loadCompanies(List.of(saved)));
    }

    /** CSV export of the filtered settlement rows (Section 9.9 report). */
    @Transactional(readOnly = true)
    public byte[] reportCsv(UUID companyId, SplitStatus status, Instant from, Instant to) {
        Specification<OrderCompanySplit> spec = Specification.where(SettlementSpecifications.company(companyId))
                .and(SettlementSpecifications.status(status))
                .and(SettlementSpecifications.paidFrom(from))
                .and(SettlementSpecifications.paidTo(to));
        List<OrderCompanySplit> splits = splitRepository.findAll(spec);
        Map<UUID, Company> companies = loadCompanies(splits);

        StringBuilder csv = new StringBuilder("order_id,company_id,company_name,bank_account,subtotal,status,paid_at,settled_at\n");
        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;
        for (OrderCompanySplit s : splits) {
            Company c = companies.get(s.getCompanyId());
            csv.append(s.getOrderId()).append(',')
               .append(s.getCompanyId()).append(',')
               .append(csv(c == null ? "" : c.getName())).append(',')
               .append(csv(c == null ? "" : c.getBankAccount())).append(',')
               .append(s.getSubtotal().toPlainString()).append(',')
               .append(s.getPaymentStatus()).append(',')
               .append(s.getPaidAt() == null ? "" : fmt.format(s.getPaidAt())).append(',')
               .append(s.getSettledAt() == null ? "" : fmt.format(s.getSettledAt())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ----- helpers -------------------------------------------------------

    private Map<UUID, Company> loadCompanies(List<OrderCompanySplit> splits) {
        Set<UUID> ids = splits.stream().map(OrderCompanySplit::getCompanyId).collect(Collectors.toSet());
        return companyRepository.findAllById(ids).stream().collect(Collectors.toMap(Company::getId, c -> c));
    }

    private SettlementDto toDto(OrderCompanySplit s, Map<UUID, Company> companies) {
        Company c = companies.get(s.getCompanyId());
        return new SettlementDto(s.getId(), s.getOrderId(), s.getCompanyId(),
                c == null ? null : c.getName(), c == null ? null : c.getBankAccount(),
                s.getSubtotal(), s.getPaymentStatus(), s.getFulfillmentStatus(),
                s.getPaidAt(), s.getSettledAt());
    }

    private static String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
