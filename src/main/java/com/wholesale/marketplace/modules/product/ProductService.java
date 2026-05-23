package com.wholesale.marketplace.modules.product;

import com.wholesale.marketplace.common.dto.PageResponse;
import com.wholesale.marketplace.common.exception.BadRequestException;
import com.wholesale.marketplace.common.exception.ResourceNotFoundException;
import com.wholesale.marketplace.modules.category.Category;
import com.wholesale.marketplace.modules.category.CategoryRepository;
import com.wholesale.marketplace.modules.company.Company;
import com.wholesale.marketplace.modules.company.CompanyRepository;
import com.wholesale.marketplace.modules.product.dto.ImportResult;
import com.wholesale.marketplace.modules.product.dto.ProductDto;
import com.wholesale.marketplace.modules.product.dto.ProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> search(UUID companyId, UUID categoryId, String search,
                                           BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> spec = Specification.where(ProductSpecifications.status(ProductStatus.ACTIVE))
                .and(ProductSpecifications.company(companyId))
                .and(ProductSpecifications.category(categoryId))
                .and(ProductSpecifications.search(search))
                .and(ProductSpecifications.minPrice(minPrice))
                .and(ProductSpecifications.maxPrice(maxPrice));
        Page<Product> page = productRepository.findAll(spec, pageable);
        return mapPage(page);
    }

    @Transactional(readOnly = true)
    public ProductDto get(UUID id) {
        return toDtoList(List.of(getEntity(id))).get(0);
    }

    @Transactional(readOnly = true)
    public Product getEntity(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Transactional(readOnly = true)
    public long countByCompany(UUID companyId) {
        return productRepository.countByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public long countByCompanyAndStatus(UUID companyId, ProductStatus status) {
        return productRepository.countByCompanyIdAndStatus(companyId, status);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> listByCompany(UUID companyId, Pageable pageable) {
        return mapPage(productRepository.findByCompanyIdAndStatus(companyId, ProductStatus.ACTIVE, pageable));
    }

    /**
     * Company-scoped catalog for the supplier portal: all statuses (incl. inactive)
     * of one company's products, with an optional name/description search.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductDto> searchForCompany(UUID companyId, String search, Pageable pageable) {
        Specification<Product> spec = Specification.where(ProductSpecifications.company(companyId))
                .and(ProductSpecifications.search(search));
        return mapPage(productRepository.findAll(spec, pageable));
    }

    @Transactional
    public ProductDto create(ProductRequest req) {
        validateRefs(req.companyId(), req.categoryId());
        Product product = Product.builder()
                .companyId(req.companyId())
                .categoryId(req.categoryId())
                .name(req.name())
                .description(req.description())
                .price(req.price())
                .minOrderQty(req.minOrderQty())
                .unit(req.unit())
                .stock(req.stock())
                .imageUrl(req.imageUrl())
                .status(ProductStatus.ACTIVE)
                .build();
        return toDtoList(List.of(productRepository.save(product))).get(0);
    }

    @Transactional
    public ProductDto update(UUID id, ProductRequest req) {
        Product product = getEntity(id);
        validateRefs(req.companyId(), req.categoryId());
        product.setCompanyId(req.companyId());
        product.setCategoryId(req.categoryId());
        product.setName(req.name());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setMinOrderQty(req.minOrderQty());
        product.setUnit(req.unit());
        product.setStock(req.stock());
        product.setImageUrl(req.imageUrl());
        return toDtoList(List.of(productRepository.save(product))).get(0);
    }

    @Transactional
    public void deactivate(UUID id) {
        Product product = getEntity(id);
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
    }

    /**
     * Bulk import for one company. Expected CSV header (order-independent):
     * {@code name,price,minOrderQty,unit,stock,description}. Fields must not contain commas.
     */
    @Transactional
    public ImportResult importCsv(UUID companyId, MultipartFile file) {
        validateRefs(companyId, null);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CSV file is required");
        }
        int imported = 0, failed = 0, lineNo = 0;
        List<String> errors = new ArrayList<>();
        List<Product> batch = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) throw new BadRequestException("CSV file is empty");
            String[] cols = Arrays.stream(header.split(",")).map(s -> s.trim().toLowerCase()).toArray(String[]::new);

            String row;
            while ((row = reader.readLine()) != null) {
                lineNo++;
                if (row.isBlank()) continue;
                try {
                    Map<String, String> values = zip(cols, row.split(",", -1));
                    Product p = Product.builder()
                            .companyId(companyId)
                            .name(require(values, "name"))
                            .description(values.getOrDefault("description", null))
                            .price(new BigDecimal(require(values, "price")))
                            .minOrderQty(Integer.parseInt(values.getOrDefault("minorderqty", "1")))
                            .unit(values.getOrDefault("unit", "unit"))
                            .stock(Integer.parseInt(values.getOrDefault("stock", "0")))
                            .status(ProductStatus.ACTIVE)
                            .build();
                    batch.add(p);
                    imported++;
                } catch (Exception ex) {
                    failed++;
                    errors.add("Line " + lineNo + ": " + ex.getMessage());
                }
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Failed to read CSV: " + ex.getMessage());
        }
        productRepository.saveAll(batch);
        return new ImportResult(imported, failed, errors);
    }

    // ----- helpers -------------------------------------------------------

    private void validateRefs(UUID companyId, UUID categoryId) {
        if (!companyRepository.existsById(companyId)) {
            throw new BadRequestException("Company does not exist: " + companyId);
        }
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new BadRequestException("Category does not exist: " + categoryId);
        }
    }

    private PageResponse<ProductDto> mapPage(Page<Product> page) {
        List<ProductDto> dtos = toDtoList(page.getContent());
        return new PageResponse<>(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /** Resolves company + category names for a batch of products in two queries (no N+1). */
    private List<ProductDto> toDtoList(List<Product> products) {
        Set<UUID> companyIds = products.stream().map(Product::getCompanyId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> categoryIds = products.stream().map(Product::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<UUID, String> companyNames = companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, Company::getName));
        Map<UUID, String> categoryNames = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return products.stream().map(p -> new ProductDto(
                p.getId(),
                p.getCompanyId(),
                companyNames.get(p.getCompanyId()),
                p.getCategoryId(),
                p.getCategoryId() == null ? null : categoryNames.get(p.getCategoryId()),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getMinOrderQty(),
                p.getUnit(),
                p.getStock(),
                p.getImageUrl(),
                p.getStatus(),
                p.getCreatedAt()
        )).toList();
    }

    private static Map<String, String> zip(String[] keys, String[] vals) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < keys.length && i < vals.length; i++) {
            String v = vals[i].trim();
            m.put(keys[i], v.isEmpty() ? null : v);
        }
        return m;
    }

    private static String require(Map<String, String> values, String key) {
        String v = values.get(key);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("missing required column '" + key + "'");
        return v;
    }
}
