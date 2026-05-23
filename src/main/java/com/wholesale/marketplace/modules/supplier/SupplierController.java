package com.wholesale.marketplace.modules.supplier;

import com.wholesale.marketplace.common.dto.PageResponse;
import com.wholesale.marketplace.modules.auth.User;
import com.wholesale.marketplace.modules.company.dto.CompanyDto;
import com.wholesale.marketplace.modules.order.OrderStatus;
import com.wholesale.marketplace.modules.order.SplitStatus;
import com.wholesale.marketplace.modules.product.dto.ImportResult;
import com.wholesale.marketplace.modules.product.dto.ProductDto;
import com.wholesale.marketplace.modules.settlement.dto.SettlementDto;
import com.wholesale.marketplace.modules.supplier.dto.SupplierDashboardDto;
import com.wholesale.marketplace.modules.supplier.dto.SupplierOrderDetailDto;
import com.wholesale.marketplace.modules.supplier.dto.SupplierOrderRowDto;
import com.wholesale.marketplace.modules.supplier.dto.SupplierProductRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Supplier portal (v1.1). Everything is scoped to the authenticated supplier's
 * own company by {@link SupplierService}; admins use the admin endpoints instead.
 */
@RestController
@RequestMapping("/api/v1/supplier")
@PreAuthorize("hasRole('SUPPLIER')")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping("/me/company")
    public CompanyDto myCompany(@AuthenticationPrincipal User user) {
        return supplierService.myCompany(user);
    }

    @GetMapping("/dashboard")
    public SupplierDashboardDto dashboard(@AuthenticationPrincipal User user) {
        return supplierService.dashboard(user);
    }

    // ----- products ------------------------------------------------------

    @GetMapping("/products")
    public PageResponse<ProductDto> products(@AuthenticationPrincipal User user,
                                             @RequestParam(required = false) String search,
                                             @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return supplierService.listProducts(user, search, pageable);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(@AuthenticationPrincipal User user,
                                    @Valid @RequestBody SupplierProductRequest req) {
        return supplierService.createProduct(user, req);
    }

    @PutMapping("/products/{id}")
    public ProductDto updateProduct(@AuthenticationPrincipal User user,
                                    @PathVariable UUID id,
                                    @Valid @RequestBody SupplierProductRequest req) {
        return supplierService.updateProduct(user, id, req);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deactivateProduct(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        supplierService.deactivateProduct(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/products/import", consumes = "multipart/form-data")
    public ImportResult importProducts(@AuthenticationPrincipal User user,
                                       @RequestParam("file") MultipartFile file) {
        return supplierService.importProducts(user, file);
    }

    // ----- orders --------------------------------------------------------

    @GetMapping("/orders")
    public PageResponse<SupplierOrderRowDto> orders(@AuthenticationPrincipal User user,
                                                    @RequestParam(required = false) OrderStatus status,
                                                    @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return supplierService.listOrders(user, status, pageable);
    }

    @GetMapping("/orders/{id}")
    public SupplierOrderDetailDto order(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return supplierService.getOrder(user, id);
    }

    // ----- settlements ---------------------------------------------------

    @GetMapping("/settlements")
    public PageResponse<SettlementDto> settlements(@AuthenticationPrincipal User user,
                                                   @RequestParam(required = false) SplitStatus status,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        return supplierService.listSettlements(user, status, pageable);
    }
}
