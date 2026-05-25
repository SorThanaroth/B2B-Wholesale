package com.wholesale.marketplace.config;

import com.wholesale.marketplace.modules.auth.Role;
import com.wholesale.marketplace.modules.auth.User;
import com.wholesale.marketplace.modules.auth.UserRepository;
import com.wholesale.marketplace.modules.auth.UserStatus;
import com.wholesale.marketplace.modules.category.Category;
import com.wholesale.marketplace.modules.category.CategoryRepository;
import com.wholesale.marketplace.modules.company.Company;
import com.wholesale.marketplace.modules.company.CompanyRepository;
import com.wholesale.marketplace.modules.company.CompanyStatus;
import com.wholesale.marketplace.modules.product.Product;
import com.wholesale.marketplace.modules.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Seeds a usable demo dataset (admin + merchant + a small catalog) the first
 * time the app starts against an empty database. Idempotent: skips if an admin
 * already exists. Disable with {@code app.seed.enabled=false}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.countByRole(Role.ADMIN) > 0) {
            // Already seeded — but backfill the demo supplier on databases that were
            // seeded before the supplier role existed (so supplier@b2b.local logs in as SUPPLIER).
            ensureDemoSupplier();
            return;
        }
        log.info("[SEED] Empty database detected — seeding demo data");

        userRepository.save(User.builder()
                .fullName("Platform Admin")
                .email("admin@b2b.local")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        userRepository.save(User.builder()
                .fullName("Demo Merchant")
                .email("merchant@b2b.local")
                .passwordHash(passwordEncoder.encode("Merchant@12345"))
                .phone("+855120000000")
                .role(Role.MERCHANT)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());

        Category beverages = categoryRepository.save(Category.builder()
                .name("Beverages").description("Water, soft drinks, juices").build());
        Category snacks = categoryRepository.save(Category.builder()
                .name("Snacks").description("Chips, biscuits, confectionery").build());

        Company water = companyRepository.save(Company.builder()
                .name("Angkor Water Co.").bankAccount("ABA-000111222")
                .contactEmail("sales@angkorwater.example").status(CompanyStatus.ACTIVE).build());
        Company snacksCo = companyRepository.save(Company.builder()
                .name("Khmer Snacks Ltd.").bankAccount("ACLEDA-333444555")
                .contactEmail("orders@khmersnacks.example").status(CompanyStatus.ACTIVE).build());

        // Admin-provisioned supplier representative for Angkor Water Co.
        userRepository.save(User.builder()
                .fullName("Angkor Water Rep")
                .email("supplier@b2b.local")
                .passwordHash(passwordEncoder.encode("Supplier@12345"))
                .phone("+855120000001")
                .role(Role.SUPPLIER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .companyId(water.getId())
                .build());

        productRepository.save(Product.builder()
                .companyId(water.getId()).categoryId(beverages.getId())
                .name("Bottled Water 500ml (carton of 24)")
                .description("Purified drinking water, 24 x 500ml bottles per carton")
                .price(new BigDecimal("3.50")).minOrderQty(10).unit("carton").stock(500).build());
        productRepository.save(Product.builder()
                .companyId(water.getId()).categoryId(beverages.getId())
                .name("Sparkling Water 1.5L (pack of 12)")
                .description("Carbonated mineral water, 12 x 1.5L bottles")
                .price(new BigDecimal("6.20")).minOrderQty(5).unit("pack").stock(200).build());
        productRepository.save(Product.builder()
                .companyId(snacksCo.getId()).categoryId(snacks.getId())
                .name("Banana Chips 100g (box of 50)")
                .description("Crispy salted banana chips, 50 packets per box")
                .price(new BigDecimal("12.00")).minOrderQty(3).unit("box").stock(150).build());
        productRepository.save(Product.builder()
                .companyId(snacksCo.getId()).categoryId(snacks.getId())
                .name("Rice Crackers 200g (box of 40)")
                .description("Roasted rice crackers, 40 packets per box")
                .price(new BigDecimal("15.75")).minOrderQty(2).unit("box").stock(120).build());

        log.info("[SEED] Done. Admin: admin@b2b.local / Admin@12345 | Merchant: merchant@b2b.local / Merchant@12345 "
                + "| Supplier: supplier@b2b.local / Supplier@12345");
    }

    /**
     * Idempotently ensures the demo SUPPLIER account exists, linked to a company
     * (prefers "Angkor Water Co."). Safe to call on every boot — no-ops if present.
     */
    private void ensureDemoSupplier() {
        if (userRepository.findByEmail("supplier@b2b.local").isPresent()) {
            return;
        }
        var companies = companyRepository.findAll();
        if (companies.isEmpty()) {
            return; // no company to attach to yet
        }
        UUID companyId = companies.stream()
                .filter(c -> "Angkor Water Co.".equals(c.getName()))
                .findFirst()
                .orElse(companies.get(0))
                .getId();

        userRepository.save(User.builder()
                .fullName("Angkor Water Rep")
                .email("supplier@b2b.local")
                .passwordHash(passwordEncoder.encode("Supplier@12345"))
                .phone("+855120000001")
                .role(Role.SUPPLIER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .companyId(companyId)
                .build());
        log.info("[SEED] Backfilled demo supplier: supplier@b2b.local / Supplier@12345 (role SUPPLIER)");
    }
}
