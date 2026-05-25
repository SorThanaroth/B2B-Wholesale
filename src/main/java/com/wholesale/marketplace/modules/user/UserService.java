package com.wholesale.marketplace.modules.user;

import com.wholesale.marketplace.common.dto.PageResponse;
import com.wholesale.marketplace.common.exception.BadRequestException;
import com.wholesale.marketplace.common.exception.ConflictException;
import com.wholesale.marketplace.common.exception.ResourceNotFoundException;
import com.wholesale.marketplace.modules.auth.Role;
import com.wholesale.marketplace.modules.auth.User;
import com.wholesale.marketplace.modules.auth.UserRepository;
import com.wholesale.marketplace.modules.auth.UserStatus;
import com.wholesale.marketplace.modules.company.Company;
import com.wholesale.marketplace.modules.company.CompanyRepository;
import com.wholesale.marketplace.modules.company.CompanyStatus;
import com.wholesale.marketplace.modules.user.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Merchant self-service (profile/password/addresses) + admin user management. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(UUID userId) {
        return toDto(load(userId));
    }

    @Transactional
    public UserProfileDto updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = load(userId);
        user.setFullName(req.fullName());
        user.setPhone(req.phone());
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        User user = load(userId);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<AddressDto> listAddresses(UUID userId) {
        return addressRepository.findByUserId(userId).stream().map(AddressDto::from).toList();
    }

    @Transactional
    public AddressDto addAddress(UUID userId, AddressRequest req) {
        boolean first = addressRepository.findByUserId(userId).isEmpty();
        boolean makeDefault = req.isDefault() || first;
        if (makeDefault) {
            addressRepository.clearDefaultForUser(userId);
        }
        Address address = Address.builder()
                .userId(userId)
                .label(req.label())
                .street(req.street())
                .city(req.city())
                .province(req.province())
                .isDefault(makeDefault)
                .build();
        return AddressDto.from(addressRepository.save(address));
    }

    @Transactional
    public AddressDto updateAddress(UUID userId, UUID addressId, AddressRequest req) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        if (req.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }
        address.setLabel(req.label());
        address.setStreet(req.street());
        address.setCity(req.city());
        address.setProvince(req.province());
        address.setDefault(req.isDefault());
        return AddressDto.from(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        addressRepository.delete(address);
    }

    // ----- admin (Section 9.10) -----------------------------------------

    /** Lists accounts of one role (MERCHANT or SUPPLIER), optionally name-filtered. */
    @Transactional(readOnly = true)
    public PageResponse<UserProfileDto> adminListByRole(Role role, String search, Pageable pageable) {
        Page<User> page = (search == null || search.isBlank())
                ? userRepository.findByRole(role, pageable)
                : userRepository.findByRoleAndFullNameContainingIgnoreCase(role, search, pageable);
        Map<UUID, String> names = companyNames(page.getContent());
        // NB: guard the null key — Map.of() (returned when no company ids) rejects get(null) with an NPE.
        return PageResponse.from(page, u ->
                UserProfileDto.from(u, u.getCompanyId() == null ? null : names.get(u.getCompanyId())));
    }

    @Transactional(readOnly = true)
    public UserProfileDto adminGet(UUID userId) {
        return toDto(load(userId));
    }

    /** Admin-creates a merchant account (active immediately, unlike self-registration). */
    @Transactional
    public UserProfileDto createMerchant(CreateMerchantRequest req) {
        String email = req.email().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = userRepository.save(User.builder()
                .fullName(req.fullName())
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .phone(req.phone())
                .role(Role.MERCHANT)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());
        return UserProfileDto.from(user);
    }

    /** Admin-provisions a supplier account bound to a company (Onboarding model: admin-provisioned). */
    @Transactional
    public UserProfileDto createSupplier(CreateSupplierRequest req) {
        String email = req.email().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        Company company = companyRepository.findById(req.companyId())
                .orElseThrow(() -> new BadRequestException("Company does not exist: " + req.companyId()));

        User user = userRepository.save(User.builder()
                .fullName(req.fullName())
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .phone(req.phone())
                .role(Role.SUPPLIER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)   // admin-provisioned, no email round-trip
                .companyId(company.getId())
                .build());
        return UserProfileDto.from(user, company.getName());
    }

    @Transactional
    public UserProfileDto assignCompany(UUID userId, UUID companyId) {
        User user = load(userId);
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BadRequestException("Company does not exist: " + companyId));
        user.setCompanyId(company.getId());
        return UserProfileDto.from(userRepository.save(user), company.getName());
    }

    @Transactional
    public UserProfileDto updateStatus(UUID userId, UserStatus status) {
        User user = load(userId);
        user.setStatus(status);
        User saved = userRepository.save(user);
        // Approving a supplier also activates their pending company so it goes live.
        if (status == UserStatus.ACTIVE && saved.getRole() == Role.SUPPLIER && saved.getCompanyId() != null) {
            companyRepository.findById(saved.getCompanyId()).ifPresent(c -> {
                if (c.getStatus() != CompanyStatus.ACTIVE) {
                    c.setStatus(CompanyStatus.ACTIVE);
                    companyRepository.save(c);
                }
            });
        }
        return toDto(saved);
    }

    @Transactional
    public UserProfileDto updateRole(UUID userId, Role role) {
        User user = load(userId);
        user.setRole(role);
        // Demoting away from SUPPLIER drops the company link to keep data consistent.
        if (role != Role.SUPPLIER) {
            user.setCompanyId(null);
        }
        return toDto(userRepository.save(user));
    }

    // ----- helpers -------------------------------------------------------

    private User load(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    /** Resolves the company name for a single user (null for non-suppliers). */
    private UserProfileDto toDto(User user) {
        String companyName = user.getCompanyId() == null
                ? null
                : companyRepository.findById(user.getCompanyId()).map(Company::getName).orElse(null);
        return UserProfileDto.from(user, companyName);
    }

    /** Batch company-name lookup for a page of users (avoids N+1). */
    private Map<UUID, String> companyNames(List<User> users) {
        Set<UUID> ids = users.stream().map(User::getCompanyId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return companyRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Company::getId, Company::getName));
    }
}
