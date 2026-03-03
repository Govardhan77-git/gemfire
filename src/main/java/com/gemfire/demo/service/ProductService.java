package com.gemfire.demo.service;

import com.gemfire.demo.model.Product;
import com.gemfire.demo.model.ProductRequest;
import com.gemfire.demo.repository.ProductRepository;
import com.gemfire.demo.util.NullSafeUtil;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.cache.execute.ResultCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service layer orchestrating all GemFire operations.
 *
 * Software Engineering Principles:
 *   SRP  — service handles business logic only; persistence delegated to repository
 *   DRY  — null-safe logic centralized in NullSafeUtil
 *   Fail Fast — input validated before any I/O
 *   Immutability — builder pattern for safe Product creation
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final Cache gemFireCache;

    public ProductService(ProductRepository productRepository, Cache gemFireCache) {
        this.productRepository = productRepository;
        this.gemFireCache = gemFireCache;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public Product createProduct(ProductRequest request) {
        request.validate();
        request.resolveDefaults();

        int stockCount = NullSafeUtil.defaultInt(request.getStockCount(), 0);

        Product product = Product.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .category(request.getCategory())
                .description(request.getDescription())
                .price(NullSafeUtil.defaultInt(request.getPrice(), 0))
                .stockCount(stockCount)
                .rating(request.getRating())
                .active(NullSafeUtil.defaultBool(request.getActive(), true))
                .featured(NullSafeUtil.defaultBool(request.getFeatured(), false))
                .inStock(NullSafeUtil.defaultBool(request.getInStock(), stockCount > 0))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy("system")
                .build();

        return productRepository.save(product);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public Product updateProduct(String id, ProductRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));

        if (request.getName() != null)        existing.setName(request.getName());
        if (request.getCategory() != null)    existing.setCategory(request.getCategory());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getPrice() != null)       existing.setPrice(request.getPrice());
        if (request.getStockCount() != null)  existing.setStockCount(request.getStockCount());
        if (request.getRating() != null)      existing.setRating(request.getRating());
        if (request.getActive() != null)      existing.setActive(request.getActive());
        if (request.getFeatured() != null)    existing.setFeatured(request.getFeatured());
        if (request.getInStock() != null)     existing.setInStock(request.getInStock());

        if (request.getInStock() == null && request.getStockCount() != null) {
            existing.setInStock(existing.getStockCount() > 0);
        }

        existing.setUpdatedAt(Instant.now());
        return productRepository.save(existing);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public Optional<Product> findById(String id) {
        return productRepository.findById(id);
    }

    public Collection<Product> findAll() {
        return productRepository.findAll();
    }

    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> findByPriceRange(int min, int max) {
        return productRepository.findByPriceRange(min, max);
    }

    public List<Product> findActiveProducts() {
        return productRepository.findActiveProducts();
    }

    public List<Product> findFeaturedProducts() {
        return productRepository.findFeaturedProducts();
    }

    public List<Product> search(String keyword) {
        return productRepository.searchByKeyword(keyword);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void deleteProduct(String id) {
        productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
        productRepository.deleteById(id);
    }

    // ── GemFire Function Execution ────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<String> adjustPricesByCategory(String category, double adjustmentPct) {
        Region<String, Product> productsRegion = gemFireCache.getRegion("Products");

        Execution<Object, List<String>, List<List<String>>> execution =
                FunctionService.onRegion(productsRegion)
                        .setArguments(new Object[]{category, adjustmentPct});

        ResultCollector<List<String>, List<List<String>>> collector =
                execution.execute("PriceAdjustmentFunction");

        List<List<String>> results = collector.getResult();
        return results.isEmpty() ? Collections.emptyList() : results.get(0);
    }

    // ── Region Stats ──────────────────────────────────────────────────────────

    public Map<String, Object> getRegionStats() {
        return productRepository.getRegionStats();
    }

    public Map<String, Object> getClusterInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("distributedSystemId",
                gemFireCache.getDistributedSystem().getDistributedMember().getId());

        Map<String, Object> regions = new LinkedHashMap<>();
        for (String name : List.of("Products", "Orders", "Sessions", "Audit")) {
            Region<?, ?> region = gemFireCache.getRegion(name);
            if (region != null) {
                regions.put(name, Map.of(
                        "size", region.size(),
                        "dataPolicy", region.getAttributes().getDataPolicy().toString(),
                        "scope", region.getAttributes().getScope().toString()
                ));
            }
        }
        info.put("regions", regions);
        return info;
    }

    // ── Seed ──────────────────────────────────────────────────────────────────

    public int seedSampleData() {
        Map<String, Product> products = new LinkedHashMap<>();
        Object[][] samples = {
            {"Laptop Pro",     "Electronics", "High-performance laptop",       1200, 50,  5, true,  true},
            {"Wireless Mouse", "Electronics", "Ergonomic wireless mouse",        45, 200, 4, true,  false},
            {"Coffee Mug",     "Kitchen",     "Ceramic coffee mug",              15,   0, 3, true,  false},
            {"Standing Desk",  "Furniture",   "Height-adjustable desk",         599,  10, 5, true,  true},
            {"Notebook",       "Stationery",  "Ruled notebook 200 pages",         8, 500, 4, true,  false},
        };
        for (Object[] s : samples) {
            String id     = UUID.randomUUID().toString();
            int stockVal  = (int) s[4];
            Product p = Product.builder()
                    .id(id)
                    .name((String) s[0])
                    .category((String) s[1])
                    .description((String) s[2])
                    .price((int) s[3])
                    .stockCount(stockVal)
                    .rating((int) s[5])
                    .active((boolean) s[6])
                    .featured((boolean) s[7])
                    .inStock(stockVal > 0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .createdBy("seed")
                    .build();
            products.put(id, p);
        }
        productRepository.saveAll(products);
        log.info("Seeded {} products via GemFire putAll", products.size());
        return products.size();
    }
}
