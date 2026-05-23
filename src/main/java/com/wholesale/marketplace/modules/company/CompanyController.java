package com.wholesale.marketplace.modules.company;

import com.wholesale.marketplace.common.dto.PageResponse;
import com.wholesale.marketplace.modules.company.dto.CompanyAdminDto;
import com.wholesale.marketplace.modules.company.dto.CompanyDto;
import com.wholesale.marketplace.modules.company.dto.CompanyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Section 9.3 — Companies. Listing is open to any merchant; mutations are admin-only. */
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public PageResponse<CompanyDto> list(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return companyService.listActive(pageable);
    }

    @GetMapping("/{id}")
    public CompanyDto get(@PathVariable UUID id) {
        return companyService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyAdminDto create(@Valid @RequestBody CompanyRequest req) {
        return companyService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyAdminDto update(@PathVariable UUID id, @Valid @RequestBody CompanyRequest req) {
        return companyService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        companyService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
