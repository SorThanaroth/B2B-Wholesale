package com.wholesale.marketplace.modules.product;

import com.wholesale.marketplace.common.dto.PageResponse;
import com.wholesale.marketplace.modules.product.dto.ImportResult;
import com.wholesale.marketplace.modules.product.dto.ProductDto;
import com.wholesale.marketplace.modules.product.dto.ProductRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

/** Section 9.5 — Products. Browsing open to merchants; mutations admin-only. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/products")
    public PageResponse<ProductDto> list(
            @RequestParam(required = false) UUID company,
            @RequestParam(required = false) UUID category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return productService.search(company, category, search, minPrice, maxPrice, pageable);
    }

    @GetMapping("/products/{id}")
    public ProductDto get(@PathVariable UUID id) {
        return productService.get(id);
    }

    @GetMapping("/companies/{id}/products")
    public PageResponse<ProductDto> byCompany(@PathVariable UUID id,
                                              @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return productService.listByCompany(id, pageable);
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody ProductRequest req) {
        return productService.create(req);
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDto update(@PathVariable UUID id, @Valid @RequestBody ProductRequest req) {
        return productService.update(id, req);
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        productService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/products/import", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ImportResult importCsv(@RequestParam UUID companyId, @RequestParam("file") MultipartFile file) {
        return productService.importCsv(companyId, file);
    }
}
