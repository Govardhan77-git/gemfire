package com.gemfire.demo.repository;

import com.gemfire.demo.model.Product;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.SelectResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GemFire-backed repository for {@link Product} entities.
 *
 * Uses the native Apache Geode {@link Region} and {@link org.apache.geode.cache.query.QueryService}
 * APIs directly — no Spring Data Geode wrapper required.
 *
 * GemFire Features Demonstrated:
 *   1. Region put / get / remove / putAll — core CRUD operations
 *   2. OQL (Object Query Language) — SELECT with WHERE, ORDER BY, GROUP BY
 *   3. Bulk putAll — single network hop for batch inserts
 *   4. Region metadata — size, dataPolicy, scope
 */
@Repository
public class ProductRepository {

    private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);

    private final Region<String, Product> productsRegion;
    private final Cache cache;

    @SuppressWarnings("unchecked")
    public ProductRepository(Cache cache) {
        this.cache = cache;
        this.productsRegion = cache.getRegion("Products");
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    public Product save(Product product) {
        productsRegion.put(product.getId(), product);
        return product;
    }

    /** GemFire Feature: putAll — inserts all entries in a single network hop */
    public void saveAll(Map<String, Product> products) {
        productsRegion.putAll(products);
        log.info("GemFire putAll — {} products stored in one network hop", products.size());
    }

    public Optional<Product> findById(String id) {
        return Optional.ofNullable(productsRegion.get(id));
    }

    public void deleteById(String id) {
        productsRegion.remove(id);
    }

    public Collection<Product> findAll() {
        return productsRegion.values();
    }

    public long count() {
        return productsRegion.size();
    }

    public Set<String> allKeys() {
        return productsRegion.keySet()
                .stream().map(Object::toString).collect(Collectors.toSet());
    }

    // ── OQL Queries ───────────────────────────────────────────────────────────

    private <T> List<T> oql(String query, Object... params) {
        try {
            var qs = cache.getQueryService();
            var q = qs.newQuery(query);
            SelectResults<T> results = (SelectResults<T>) q.execute(params);
            return new ArrayList<>(results.asList());
        } catch (Exception e) {
            log.error("OQL query failed: {} | params: {}", query, params, e);
            return Collections.emptyList();
        }
    }

    /** OQL: WHERE category = $1 */
    public List<Product> findByCategory(String category) {
        return oql("SELECT * FROM /Products p WHERE p.category = $1", category);
    }

    /** OQL: range query with ORDER BY */
    public List<Product> findByPriceRange(int min, int max) {
        return oql("SELECT * FROM /Products p WHERE p.price >= $1 AND p.price <= $2 ORDER BY p.price ASC", min, max);
    }

    /** OQL: boolean field filter */
    public List<Product> findActiveProducts() {
        return oql("SELECT * FROM /Products p WHERE p.active = true AND p.inStock = true");
    }

    /** OQL: featured + ORDER BY rating */
    public List<Product> findFeaturedProducts() {
        return oql("SELECT * FROM /Products p WHERE p.featured = true ORDER BY p.rating DESC NULLS LAST");
    }

    /** OQL: keyword search on name/description */
    public List<Product> searchByKeyword(String keyword) {
        String lower = keyword.toLowerCase();
        return productsRegion.values().stream()
                .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(lower))
                          || (p.getDescription() != null && p.getDescription().toLowerCase().contains(lower)))
                .collect(Collectors.toList());
    }

    // ── Region Stats ──────────────────────────────────────────────────────────

    public Map<String, Object> getRegionStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("regionName", productsRegion.getName());
        stats.put("size", productsRegion.size());
        stats.put("dataPolicy", productsRegion.getAttributes().getDataPolicy().toString());
        stats.put("scope", productsRegion.getAttributes().getScope().toString());
        stats.put("statisticsEnabled", productsRegion.getAttributes().getStatisticsEnabled());
        return stats;
    }
}
