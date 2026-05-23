package com.wholesale.marketplace.modules.order;

import com.wholesale.marketplace.common.dto.PageResponse;
import com.wholesale.marketplace.common.exception.BadRequestException;
import com.wholesale.marketplace.common.exception.ResourceNotFoundException;
import com.wholesale.marketplace.modules.cart.Cart;
import com.wholesale.marketplace.modules.cart.CartItem;
import com.wholesale.marketplace.modules.cart.CartService;
import com.wholesale.marketplace.modules.company.Company;
import com.wholesale.marketplace.modules.company.CompanyRepository;
import com.wholesale.marketplace.modules.order.dto.*;
import com.wholesale.marketplace.modules.payment.PaymentService;
import com.wholesale.marketplace.modules.payment.dto.QrResponse;
import com.wholesale.marketplace.modules.product.Product;
import com.wholesale.marketplace.modules.product.ProductRepository;
import com.wholesale.marketplace.modules.product.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Order lifecycle. Checkout atomically creates the Order, its line items, and
 * one settlement split per company, decrements stock, closes the cart, and
 * generates the single unified QR (Section 8).
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderCompanySplitRepository splitRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final PaymentService paymentService;
    private final InvoicePdfGenerator invoicePdfGenerator;

    @Transactional
    public QrResponse checkout(UUID userId) {
        Cart cart = cartService.getOrCreateActiveCart(userId);
        List<CartItem> cartItems = cartService.itemsOf(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }
        Map<UUID, Product> products = productRepository.findAllById(
                        cartItems.stream().map(CartItem::getProductId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        // Re-validate against current product state, then total up.
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : cartItems) {
            Product p = products.get(ci.getProductId());
            if (p == null || p.getStatus() != ProductStatus.ACTIVE) {
                throw new BadRequestException("A product in your cart is no longer available");
            }
            if (ci.getQuantity() < p.getMinOrderQty()) {
                throw new BadRequestException("'" + p.getName() + "' is below its minimum order quantity");
            }
            if (ci.getQuantity() > p.getStock()) {
                throw new BadRequestException("Insufficient stock for '" + p.getName() + "'");
            }
            total = total.add(ci.getSubtotal());
        }

        Order order = orderRepository.save(Order.builder()
                .userId(userId)
                .cartId(cart.getId())
                .totalAmount(total)
                .status(OrderStatus.PENDING)
                .qrToken(UUID.randomUUID().toString().replace("-", ""))
                .build());

        // Line items (snapshotted) + per-company subtotals.
        List<OrderItem> items = new ArrayList<>();
        Map<UUID, BigDecimal> companyTotals = new LinkedHashMap<>();
        for (CartItem ci : cartItems) {
            Product p = products.get(ci.getProductId());
            items.add(OrderItem.builder()
                    .orderId(order.getId())
                    .productId(p.getId())
                    .companyId(p.getCompanyId())
                    .productName(p.getName())
                    .quantity(ci.getQuantity())
                    .unitPrice(ci.getUnitPrice())
                    .subtotal(ci.getSubtotal())
                    .build());
            companyTotals.merge(p.getCompanyId(), ci.getSubtotal(), BigDecimal::add);
            p.setStock(p.getStock() - ci.getQuantity());   // reserve stock
        }
        orderItemRepository.saveAll(items);
        productRepository.saveAll(products.values());

        List<OrderCompanySplit> splits = companyTotals.entrySet().stream()
                .map(e -> OrderCompanySplit.builder()
                        .orderId(order.getId())
                        .companyId(e.getKey())
                        .subtotal(e.getValue())
                        .paymentStatus(SplitStatus.PENDING)
                        .build())
                .toList();
        splitRepository.saveAll(splits);

        cartService.markCheckedOut(cart);   // next access starts a fresh active cart
        return paymentService.createForOrder(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryDto> myOrders(UUID userId, Pageable pageable) {
        return PageResponse.from(orderRepository.findByUserId(userId, pageable), OrderSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public OrderDetailDto getMyOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return toDetail(order);
    }

    @Transactional(readOnly = true)
    public byte[] invoicePdf(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return invoicePdfGenerator.generate(toDetail(order));
    }

    // ----- admin ---------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryDto> adminList(OrderStatus status, UUID companyId,
                                                   Instant from, Instant to, Pageable pageable) {
        Specification<Order> spec = Specification.where(OrderSpecifications.status(status))
                .and(OrderSpecifications.containsCompany(companyId))
                .and(OrderSpecifications.createdFrom(from))
                .and(OrderSpecifications.createdTo(to));
        Page<Order> page = orderRepository.findAll(spec, pageable);
        return PageResponse.from(page, OrderSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public OrderDetailDto adminGet(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return toDetail(order);
    }

    @Transactional
    public OrderSummaryDto updateStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setStatus(status);
        return OrderSummaryDto.from(orderRepository.save(order));
    }

    // ----- helpers -------------------------------------------------------

    private OrderDetailDto toDetail(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderCompanySplit> splits = splitRepository.findByOrderId(order.getId());

        Set<UUID> companyIds = new HashSet<>();
        items.forEach(i -> companyIds.add(i.getCompanyId()));
        splits.forEach(s -> companyIds.add(s.getCompanyId()));
        Map<UUID, String> companyNames = companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, Company::getName));

        List<OrderItemDto> itemDtos = items.stream().map(i -> new OrderItemDto(
                i.getId(), i.getProductId(), i.getProductName(), i.getCompanyId(),
                companyNames.get(i.getCompanyId()), i.getQuantity(), i.getUnitPrice(), i.getSubtotal())).toList();

        List<OrderSplitDto> splitDtos = splits.stream().map(s -> new OrderSplitDto(
                s.getId(), s.getCompanyId(), companyNames.get(s.getCompanyId()), s.getSubtotal(),
                s.getPaymentStatus(), s.getPaidAt(), s.getSettledAt())).toList();

        return new OrderDetailDto(order.getId(), order.getUserId(), order.getTotalAmount(),
                order.getStatus(), order.getPaymentStatus(), order.getQrToken(), order.getCreatedAt(),
                itemDtos, splitDtos);
    }
}
