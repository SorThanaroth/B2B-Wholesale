package com.wholesale.marketplace.modules.cart;

import com.wholesale.marketplace.modules.auth.User;
import com.wholesale.marketplace.modules.cart.dto.AddItemRequest;
import com.wholesale.marketplace.modules.cart.dto.CartDto;
import com.wholesale.marketplace.modules.cart.dto.UpdateItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Section 9.6 — Cart. Always scoped to the authenticated merchant's active cart. */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartDto getCart(@AuthenticationPrincipal User user) {
        return cartService.getCart(user.getId());
    }

    @PostMapping("/items")
    public CartDto addItem(@AuthenticationPrincipal User user, @Valid @RequestBody AddItemRequest req) {
        return cartService.addItem(user.getId(), req);
    }

    @PutMapping("/items/{id}")
    public CartDto updateItem(@AuthenticationPrincipal User user,
                              @PathVariable UUID id,
                              @Valid @RequestBody UpdateItemRequest req) {
        return cartService.updateItem(user.getId(), id, req);
    }

    @DeleteMapping("/items/{id}")
    public CartDto removeItem(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return cartService.removeItem(user.getId(), id);
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@AuthenticationPrincipal User user) {
        cartService.clear(user.getId());
        return ResponseEntity.noContent().build();
    }
}
