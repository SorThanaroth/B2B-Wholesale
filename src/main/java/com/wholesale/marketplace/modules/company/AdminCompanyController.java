package com.wholesale.marketplace.modules.company;

import com.wholesale.marketplace.common.dto.PageResponse;
import com.wholesale.marketplace.modules.company.dto.CompanyAdminDto;
import com.wholesale.marketplace.modules.company.dto.UpdateCompanyStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin company management. Lists ALL companies (active + inactive) and toggles
 * status — deactivation is a status change, never a delete, so the company stays
 * visible and can be reactivated.
 */
@RestController
@RequestMapping("/api/v1/admin/companies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCompanyController {

    private final CompanyService companyService;

    @GetMapping
    public PageResponse<CompanyAdminDto> list(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return companyService.listAll(pageable);
    }

    @PutMapping("/{id}/status")
    public CompanyAdminDto setStatus(@PathVariable UUID id, @Valid @RequestBody UpdateCompanyStatusRequest req) {
        return companyService.setStatus(id, req.status());
    }
}
