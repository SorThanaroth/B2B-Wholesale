package com.wholesale.marketplace.modules.company;

import com.wholesale.marketplace.common.dto.PageResponse;
import com.wholesale.marketplace.common.exception.ResourceNotFoundException;
import com.wholesale.marketplace.modules.company.dto.CompanyAdminDto;
import com.wholesale.marketplace.modules.company.dto.CompanyDto;
import com.wholesale.marketplace.modules.company.dto.CompanyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public PageResponse<CompanyDto> listActive(Pageable pageable) {
        return PageResponse.from(companyRepository.findByStatus(CompanyStatus.ACTIVE, pageable), CompanyDto::from);
    }

    @Transactional(readOnly = true)
    public CompanyDto get(UUID id) {
        return CompanyDto.from(getEntity(id));
    }

    /** Used by other modules (products, orders) that need the company aggregate. */
    @Transactional(readOnly = true)
    public Company getEntity(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    @Transactional
    public CompanyAdminDto create(CompanyRequest req) {
        Company company = Company.builder()
                .name(req.name())
                .logoUrl(req.logoUrl())
                .bankAccount(req.bankAccount())
                .contactEmail(req.contactEmail())
                .status(CompanyStatus.ACTIVE)
                .build();
        return CompanyAdminDto.from(companyRepository.save(company));
    }

    @Transactional
    public CompanyAdminDto update(UUID id, CompanyRequest req) {
        Company company = getEntity(id);
        company.setName(req.name());
        company.setLogoUrl(req.logoUrl());
        company.setBankAccount(req.bankAccount());
        company.setContactEmail(req.contactEmail());
        return CompanyAdminDto.from(companyRepository.save(company));
    }

    @Transactional
    public void deactivate(UUID id) {
        Company company = getEntity(id);
        company.setStatus(CompanyStatus.INACTIVE);
        companyRepository.save(company);
    }
}
