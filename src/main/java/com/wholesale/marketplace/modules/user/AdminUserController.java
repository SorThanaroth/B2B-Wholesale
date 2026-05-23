package com.wholesale.marketplace.modules.user;

import com.wholesale.marketplace.common.dto.PageResponse;
import com.wholesale.marketplace.modules.auth.Role;
import com.wholesale.marketplace.modules.user.dto.AssignCompanyRequest;
import com.wholesale.marketplace.modules.user.dto.CreateMerchantRequest;
import com.wholesale.marketplace.modules.user.dto.CreateSupplierRequest;
import com.wholesale.marketplace.modules.user.dto.UpdateUserRoleRequest;
import com.wholesale.marketplace.modules.user.dto.UpdateUserStatusRequest;
import com.wholesale.marketplace.modules.user.dto.UserProfileDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Section 9.10 — Admin user management (merchants + supplier provisioning). */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    /** Lists accounts by role (defaults to MERCHANT to preserve existing behaviour). */
    @GetMapping
    public PageResponse<UserProfileDto> list(@RequestParam(required = false) String search,
                                             @RequestParam(defaultValue = "MERCHANT") Role role,
                                             @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return userService.adminListByRole(role, search, pageable);
    }

    @GetMapping("/{id}")
    public UserProfileDto get(@PathVariable UUID id) {
        return userService.adminGet(id);
    }

    /** Creates a MERCHANT account (active immediately). */
    @PostMapping("/merchant")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileDto createMerchant(@Valid @RequestBody CreateMerchantRequest req) {
        return userService.createMerchant(req);
    }

    /** Creates a SUPPLIER account bound to a company. */
    @PostMapping("/supplier")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileDto createSupplier(@Valid @RequestBody CreateSupplierRequest req) {
        return userService.createSupplier(req);
    }

    /** Re-points a supplier account at a different company. */
    @PutMapping("/{id}/company")
    public UserProfileDto assignCompany(@PathVariable UUID id, @Valid @RequestBody AssignCompanyRequest req) {
        return userService.assignCompany(id, req.companyId());
    }

    @PutMapping("/{id}/status")
    public UserProfileDto updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateUserStatusRequest req) {
        return userService.updateStatus(id, req.status());
    }

    @PutMapping("/{id}/role")
    public UserProfileDto updateRole(@PathVariable UUID id, @Valid @RequestBody UpdateUserRoleRequest req) {
        return userService.updateRole(id, req.role());
    }
}
