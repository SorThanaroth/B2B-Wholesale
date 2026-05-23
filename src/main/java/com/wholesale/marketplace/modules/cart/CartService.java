package com.wholesale.marketplace.modules.cart;

import com.wholesale.marketplace.common.exception.BadRequestException;
import com.wholesale.marketplace.common.exception.ResourceNotFoundException;
import com.wholesale.marketplace.modules.cart.dto.*;
import com.wholesale.marketplace.modules.company.Company;
import com.wholesale.marketplace.modules.company.CompanyRepository;
import com.wholesale.marketplace.modules.product.Product;
import com.wholesale.marketplace.modules.product.ProductRepository;
import com.wholesale.marketplace.modules.product.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cart management. Enforces the wholesale rule: an item's quantity may never
 * drop below the product's {@code minOrderQty}. Subtotals are grouped per
 * supplier company so the merchant sees what each company is owed.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public CartDto getCart(UUID userId) {
        return toDto(getOrCreateActiveCart(userId));
    }

    @Transactional
    public CartDto addItem(UUID userId, AddItemRequest req) {
        Cart cart = getOrCreateActiveCart(userId);
        Product product = activeProduct(req.productId());

        CartItem existing = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()).orElse(null);
        int newQty = (existing == null ? 0 : existing.getQuantity()) + req.quantity();
        validateQuantity(product, newQty);

        if (existing == null) {
            cartItemRepository.save(CartItem.builder()
                    .cartId(cart.getId())
                    .productId(product.getId())
                    .quantity(newQty)
                    .unitPrice(product.getPrice())
                    .subtotal(lineTotal(product.getPrice(), newQty))
                    .build());
        } else {
            existing.setQuantity(newQty);
            existing.setUnitPrice(product.getPrice());
            existing.setSubtotal(lineTotal(product.getPrice(), newQty));
            cartItemRepository.save(existing);
        }
        return toDto(cart);
    }

    @Transactional
    public CartDto updateItem(UUID userId, UUID itemId, UpdateItemRequest req) {
        Cart cart = getOrCreateActiveCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId));
        Product product = activeProduct(item.getProductId());
        validateQuantity(product, req.quantity());

        item.setQuantity(req.quantity());
        item.setUnitPrice(product.getPrice());
        item.setSubtotal(lineTotal(product.getPrice(), req.quantity()));
        cartItemRepository.save(item);
        return toDto(cart);
    }

    @Transactional
    public CartDto removeItem(UUID userId, UUID itemId) {
        Cart cart = getOrCreateActiveCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId));
        cartItemRepository.delete(item);
        return toDto(cart);
    }

    @Transactional
    public void clear(UUID userId) {
        Cart cart = getOrCreateActiveCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    // ----- collaboration with checkout ----------------------------------

    @Transactional
    public Cart getOrCreateActiveCart(UUID userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .userId(userId).status(CartStatus.ACTIVE).build()));
    }

    @Transactional(readOnly = true)
    public List<CartItem> itemsOf(UUID cartId) {
        return cartItemRepository.findByCartId(cartId);
    }

    @Transactional
    public void markCheckedOut(Cart cart) {
        cart.setStatus(CartStatus.CHECKED_OUT);
        cartRepository.save(cart);
    }

    // ----- helpers -------------------------------------------------------

    private Product activeProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("Product is not available: " + product.getName());
        }
        return product;
    }

    private void validateQuantity(Product product, int qty) {
        if (qty < product.getMinOrderQty()) {
            throw new BadRequestException("Minimum wholesale quantity for '" + product.getName()
                    + "' is " + product.getMinOrderQty() + " " + product.getUnit());
        }
        if (qty > product.getStock()) {
            throw new BadRequestException("Only " + product.getStock() + " " + product.getUnit()
                    + " of '" + product.getName() + "' in stock");
        }
    }

    private static BigDecimal lineTotal(BigDecimal unitPrice, int qty) {
        return unitPrice.multiply(BigDecimal.valueOf(qty));
    }

    /** Builds the grouped cart view (items by company, per-company + grand totals). */
    private CartDto toDto(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) {
            return new CartDto(cart.getId(), List.of(), BigDecimal.ZERO, 0);
        }
        Map<UUID, Product> products = productRepository.findAllById(
                        items.stream().map(CartItem::getProductId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Product::getId, p -> p));
        Map<UUID, String> companyNames = companyRepository.findAllById(
                        products.values().stream().map(Product::getCompanyId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Company::getId, Company::getName));

        // companyId -> list of line DTOs (preserve insertion order)
        Map<UUID, List<CartItemDto>> byCompany = new LinkedHashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItem item : items) {
            Product p = products.get(item.getProductId());
            if (p == null) continue; // product removed since added
            CartItemDto line = new CartItemDto(item.getId(), p.getId(), p.getName(), p.getUnit(),
                    item.getUnitPrice(), item.getQuantity(), p.getMinOrderQty(), item.getSubtotal());
            byCompany.computeIfAbsent(p.getCompanyId(), k -> new ArrayList<>()).add(line);
            grandTotal = grandTotal.add(item.getSubtotal());
            totalItems += item.getQuantity();
        }

        List<CartCompanyGroup> groups = new ArrayList<>();
        for (var entry : byCompany.entrySet()) {
            BigDecimal companySubtotal = entry.getValue().stream()
                    .map(CartItemDto::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            groups.add(new CartCompanyGroup(entry.getKey(),
                    companyNames.get(entry.getKey()), companySubtotal, entry.getValue()));
        }
        return new CartDto(cart.getId(), groups, grandTotal, totalItems);
    }
}
