package com.wholesale.marketplace.modules.user;

import com.wholesale.marketplace.modules.auth.User;
import com.wholesale.marketplace.modules.user.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Sections 9.2 — User / Profile (the authenticated merchant acting on themselves). */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public UserProfileDto getProfile(@AuthenticationPrincipal User user) {
        return userService.getProfile(user.getId());
    }

    @PutMapping
    public UserProfileDto updateProfile(@AuthenticationPrincipal User user,
                                        @Valid @RequestBody UpdateProfileRequest req) {
        return userService.updateProfile(user.getId(), req);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User user,
                                               @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(user.getId(), req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/addresses")
    public List<AddressDto> listAddresses(@AuthenticationPrincipal User user) {
        return userService.listAddresses(user.getId());
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDto addAddress(@AuthenticationPrincipal User user,
                                 @Valid @RequestBody AddressRequest req) {
        return userService.addAddress(user.getId(), req);
    }

    @PutMapping("/addresses/{id}")
    public AddressDto updateAddress(@AuthenticationPrincipal User user,
                                    @PathVariable UUID id,
                                    @Valid @RequestBody AddressRequest req) {
        return userService.updateAddress(user.getId(), id, req);
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        userService.deleteAddress(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
