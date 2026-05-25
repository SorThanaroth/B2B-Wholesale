package com.wholesale.marketplace.modules.supplier;

import com.wholesale.marketplace.common.dto.PageResponse;
import com.wholesale.marketplace.common.exception.BadRequestException;
import com.wholesale.marketplace.common.exception.ResourceNotFoundException;
import com.wholesale.marketplace.modules.auth.User;
import com.wholesale.marketplace.modules.company.Company;
import com.wholesale.marketplace.modules.company.CompanyRepository;
import com.wholesale.marketplace.modules.company.dto.CompanyDto;
import com.wholesale.marketplace.modules.order.*;
import com.wholesale.marketplace.modules.order.dto.OrderItemDto;
import com.wholesale.marketplace.modules.product.Product;
import com.wholesale.marketplace.modules.product.ProductService;
import com.wholesale.marketplace.modules.product.ProductStatus;
import com.wholesale.marketplace.modules.product.dto.ImportResult;
import com.wholesale.marketplace.modules.product.dto.ProductDto;
import com.wholesale.marketplace.modules.settlement.SettlementService;
import com.wholesale.marketplace.modules.settlement.dto.SettlementDto;
import com.wholesale.marketplace.modules.supplier.dto.SupplierDashboardDto;
import com.wholesale.marketplace.modules.supplier.dto.SupplierOrderDetailDto;
import com.wholesale.marketplace.modules.supplier.dto.SupplierOrderRowDto;
import com.wholesale.marketplace.modules.supplier.dto.SupplierProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Supplier portal logic (v1.1 / report §10 v1.3). Every operation is scoped to
 * the authenticated supplier's own company — a supplier can never read or mutate
 * another company's products, orders or settlements.
 */
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final ProductService productService;
    private final OrderService orderService;
    private final CompanyRepository companyRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderCompanySplitRepository splitRepository;
    private final SettlementService settlementService;

    // ----- company -------------------------------------------------------

    @Transactional(readOnly = true)
    public CompanyDto myCompany(User user) {
        return CompanyDto.from(loadCompany(requireCompanyId(user)));
    }

    @Transactional(readOnly = true)
    public SupplierDashboardDto dashboard(User user) {
        UUID companyId = requireCompanyId(user);
        return new SupplierDashboardDto(
                productService.countByCompany(companyId),
                productService.countByCompanyAndStatus(companyId, ProductStatus.ACTIVE),
                splitRepository.countByCompanyId(companyId),
                splitRepository.sumSubtotalByCompanyAndStatus(companyId, SplitStatus.PENDING_SETTLEMENT),
                splitRepository.sumSubtotalByCompanyAndStatus(companyId, SplitStatus.SETTLED),
                ordersPage(companyId, null, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .content());
    }

    // ----- products ------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> listProducts(User user, String search, Pageable pageable) {
        return productService.searchForCompany(requireCompanyId(user), search, pageable);
    }

    @Transactional
    public ProductDto createProduct(User user, SupplierProductRequest req) {
        UUID companyId = requireCompanyId(user);
        return productService.create(req.toProductRequest(companyId));
    }

    @Transactional
    public ProductDto updateProduct(User user, UUID productId, SupplierProductRequest req) {
        UUID companyId = requireCompanyId(user);
        assertOwnsProduct(companyId, productId);
        return productService.update(productId, req.toProductRequest(companyId));
    }

    @Transactional
    public void deactivateProduct(User user, UUID productId) {
        UUID companyId = requireCompanyId(user);
        assertOwnsProduct(companyId, productId);
        productService.deactivate(productId);
    }

    @Transactional
    public ImportResult importProducts(User user, MultipartFile file) {
        return productService.importCsv(requireCompanyId(user), file);
    }

    // ----- orders --------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<SupplierOrderRowDto> listOrders(User user, OrderStatus status, Pageable pageable) {
        return ordersPage(requireCompanyId(user), status, pageable);
    }

    @Transactional(readOnly = true)
    public SupplierOrderDetailDto getOrder(User user, UUID orderId) {
        UUID companyId = requireCompanyId(user);
        OrderCompanySplit split = splitRepository.findByOrderIdAndCompanyId(orderId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        String companyName = loadCompany(companyId).getName();
        List<OrderItemDto> items = orderItemRepository.findByOrderId(orderId).stream()
                .filter(i -> companyId.equals(i.getCompanyId()))
                .map(i -> new OrderItemDto(i.getId(), i.getProductId(), i.getProductName(),
                        i.getCompanyId(), companyName, i.getQuantity(), i.getUnitPrice(), i.getSubtotal()))
                .toList();

        return new SupplierOrderDetailDto(order.getId(), split.getId(), order.getCreatedAt(),
                order.getStatus(), order.getPaymentStatus(), split.getSubtotal(), split.getPaymentStatus(),
                split.getFulfillmentStatus(), split.getPaidAt(), split.getSettledAt(), items);
    }

    /**
     * Supplier updates the delivery status of their own share of an order
     * (PROCESSING/SHIPPED only — confirming arrival is the merchant's action).
     */
    @Transactional
    public SupplierOrderDetailDto updateFulfillment(User user, UUID orderId, FulfillmentStatus status) {
        if (status == FulfillmentStatus.DELIVERED) {
            throw new BadRequestException("Only the merchant can confirm delivery (arrival)");
        }
        UUID companyId = requireCompanyId(user);
        OrderCompanySplit split = splitRepository.findByOrderIdAndCompanyId(orderId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        orderService.setSplitFulfillment(split.getId(), status);
        return getOrder(user, orderId);
    }

    // ----- settlements ---------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<SettlementDto> listSettlements(User user, SplitStatus status, Pageable pageable) {
        return settlementService.list(requireCompanyId(user), status, null, null, pageable);
    }

    // ----- helpers -------------------------------------------------------

    private PageResponse<SupplierOrderRowDto> ordersPage(UUID companyId, OrderStatus status, Pageable pageable) {
        Specification<Order> spec = Specification.where(OrderSpecifications.containsCompany(companyId))
                .and(OrderSpecifications.status(status));
        Page<Order> page = orderRepository.findAll(spec, pageable);

        List<UUID> orderIds = page.getContent().stream().map(Order::getId).toList();
        Map<UUID, OrderCompanySplit> splitByOrder = orderIds.isEmpty()
                ? Map.of()
                : splitRepository.findByCompanyIdAndOrderIdIn(companyId, orderIds).stream()
                        .collect(Collectors.toMap(OrderCompanySplit::getOrderId, s -> s));

        List<SupplierOrderRowDto> rows = page.getContent().stream().map(o -> {
            OrderCompanySplit s = splitByOrder.get(o.getId());
            return new SupplierOrderRowDto(
                    o.getId(), o.getCreatedAt(), o.getStatus(), o.getPaymentStatus(),
                    s == null ? BigDecimal.ZERO : s.getSubtotal(),
                    s == null ? null : s.getPaymentStatus(),
                    s == null ? null : s.getFulfillmentStatus(),
                    s == null ? null : s.getId(),
                    s == null ? null : s.getPaidAt());
        }).toList();

        return new PageResponse<>(rows, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    private void assertOwnsProduct(UUID companyId, UUID productId) {
        Product product = productService.getEntity(productId);
        if (!companyId.equals(product.getCompanyId())) {
            // Don't reveal another company's product — treat as not found for this supplier.
            throw new ResourceNotFoundException("Product", productId);
        }
    }

    private Company loadCompany(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
    }

    private UUID requireCompanyId(User user) {
        if (user.getCompanyId() == null) {
            throw new BadRequestException("This supplier account is not linked to a company yet");
        }
        return user.getCompanyId();
    }
}
