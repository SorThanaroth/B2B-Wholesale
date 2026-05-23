package com.wholesale.marketplace.modules.auth;

import com.wholesale.marketplace.common.exception.BadRequestException;
import com.wholesale.marketplace.common.exception.ConflictException;
import com.wholesale.marketplace.config.JwtService;
import com.wholesale.marketplace.modules.auth.dto.*;
import com.wholesale.marketplace.modules.company.Company;
import com.wholesale.marketplace.modules.company.CompanyRepository;
import com.wholesale.marketplace.modules.company.CompanyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication & account-recovery logic. Access tokens are stateless JWTs;
 * refresh tokens are opaque, persisted, and rotated on use. Email delivery is
 * simulated (the verification/reset link is logged rather than emailed).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days
    private long refreshExpirationMillis;

    @Value("${jwt.verification-expiration:86400000}") // 24h
    private long verificationExpirationMillis;

    /**
     * Self-registration with role selection. A registrant chooses MERCHANT
     * (buyer) or SUPPLIER (seller); a SUPPLIER also creates their company.
     * All self-registered accounts (and the supplier's new company) start
     * PENDING/INACTIVE and require admin approval before they can sign in.
     */
    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        Role role = request.role() == null ? Role.MERCHANT : request.role();
        if (role == Role.ADMIN) {
            throw new BadRequestException("Admin accounts cannot be self-registered");
        }
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new ConflictException("An account with this email already exists");
        }

        UUID companyId = null;
        if (role == Role.SUPPLIER) {
            // Supplier == company: create the (pending) company they will represent.
            if (request.companyName() == null || request.companyName().isBlank()) {
                throw new BadRequestException("Company name is required to register as a supplier");
            }
            if (request.bankAccount() == null || request.bankAccount().isBlank()) {
                throw new BadRequestException("Settlement bank account is required to register as a supplier");
            }
            Company company = companyRepository.save(Company.builder()
                    .name(request.companyName())
                    .bankAccount(request.bankAccount())
                    .contactEmail(request.contactEmail())
                    .status(CompanyStatus.INACTIVE)   // activated when admin approves the supplier
                    .build());
            companyId = company.getId();
        }

        User user = userRepository.save(User.builder()
                .fullName(request.fullName())
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(role)
                .status(UserStatus.PENDING)         // awaiting admin approval
                .emailVerified(false)
                .companyId(companyId)
                .build());

        issueAndLogVerificationLink(user, VerificationTokenType.EMAIL_VERIFY, "/api/v1/auth/verify-email?token=");
        return new RegistrationResponse(
                "Registration received. Your account is pending admin approval — "
                        + "you'll be able to sign in once it's approved.",
                role, user.getStatus());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (!stored.isActive()) {
            throw new BadRequestException("Refresh token expired or revoked");
        }
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BadRequestException("Account no longer exists"));

        // Rotate: revoke the used token, mint a fresh pair.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken()).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken vt = consumeToken(token, VerificationTokenType.EMAIL_VERIFY);
        User user = userRepository.findById(vt.getUserId())
                .orElseThrow(() -> new BadRequestException("Account no longer exists"));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    /** Always returns 200 to avoid leaking which emails are registered. */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email().toLowerCase()).ifPresent(user ->
                issueAndLogVerificationLink(user, VerificationTokenType.PASSWORD_RESET,
                        "/api/v1/auth/reset-password?token="));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationToken vt = consumeToken(request.token(), VerificationTokenType.PASSWORD_RESET);
        User user = userRepository.findById(vt.getUserId())
                .orElseThrow(() -> new BadRequestException("Account no longer exists"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        // Force re-login everywhere.
        refreshTokenRepository.revokeAllForUser(user.getId());
    }

    // ----- helpers -------------------------------------------------------

    private AuthResponse buildAuthResponse(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("uid", user.getId().toString());
        String accessToken = jwtService.generateToken(claims, user);

        RefreshToken refresh = RefreshToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID() + "." + UUID.randomUUID())
                .expiresAt(Instant.now().plusMillis(refreshExpirationMillis))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refresh);

        return new AuthResponse(
                accessToken,
                refresh.getToken(),
                "Bearer",
                jwtService.getAccessExpirationMillis() / 1000,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCompanyId());
    }

    private void issueAndLogVerificationLink(User user, VerificationTokenType type, String pathPrefix) {
        VerificationToken vt = VerificationToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID().toString().replace("-", ""))
                .type(type)
                .expiresAt(Instant.now().plusMillis(verificationExpirationMillis))
                .used(false)
                .build();
        verificationTokenRepository.save(vt);
        // Simulated email delivery.
        log.info("[MOCK EMAIL] {} link for {}: {}{}", type, user.getEmail(), pathPrefix, vt.getToken());
    }

    private VerificationToken consumeToken(String token, VerificationTokenType type) {
        VerificationToken vt = verificationTokenRepository.findByTokenAndType(token, type)
                .orElseThrow(() -> new BadRequestException("Invalid or unknown token"));
        if (!vt.isUsable()) {
            throw new BadRequestException("Token expired or already used");
        }
        vt.setUsed(true);
        // expiresAt comparison uses truncated instants consistently
        vt.setExpiresAt(vt.getExpiresAt().truncatedTo(ChronoUnit.MILLIS));
        verificationTokenRepository.save(vt);
        return vt;
    }
}
