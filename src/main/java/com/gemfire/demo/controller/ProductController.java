package com.gemfire.demo.controller;

import com.gemfire.demo.model.CommonResponseDTO;
import com.gemfire.demo.model.Product;
import com.gemfire.demo.model.ProductRequest;
import com.gemfire.demo.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Controller for Product operations.
 *
 * <p>Exposes endpoints demonstrating all GemFire features:
 * CRUD, OQL queries, Function execution, CQ triggers, and stats.
 *
 * <p>Follows REST best practices: proper HTTP verbs, status codes,
 * and uniform response envelope via {@link CommonResponseDTO}.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "GemFire-backed product management API")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ── CRUD ────────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a product",
               description = "Stores a product in GemFire REPLICATE region. " +
                             "Null/empty int and boolean fields are handled gracefully " +
                             "— defaults applied before persistence.")
    public ResponseEntity<CommonResponseDTO<Product>> create(@RequestBody ProductRequest request) {
        log.info("POST /products | name={}, price={}, active={}, inStock={}",
                request.getName(), request.getPrice(), request.getActive(), request.getInStock());
        Product created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponseDTO.ok(created, "Product created in GemFire"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID",
               description = "GemFire Feature: Single-hop read from REPLICATE region. " +
                             "Also uses @Cacheable to demonstrate Spring Cache + GemFire integration.")
    public ResponseEntity<CommonResponseDTO<Product>> getById(@PathVariable String id) {
        return productService.findById(id)
                .map(p -> ResponseEntity.ok(CommonResponseDTO.ok(p, "Fetched the details")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CommonResponseDTO.error("Product not found: " + id)));
    }

    @GetMapping
    @Operation(summary = "Get all products",
               description = "Returns all entries from GemFire /Products region.")
    public ResponseEntity<CommonResponseDTO<Collection<Product>>> getAll() {
        return ResponseEntity.ok(CommonResponseDTO.ok(productService.findAll(), "Fetched the details"));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partial update",
               description = "Merge update — only non-null fields overwrite existing values. " +
                             "Null int/boolean fields in payload do NOT reset stored values.")
    public ResponseEntity<CommonResponseDTO<Product>> update(
            @PathVariable String id,
            @RequestBody ProductRequest request) {
        try {
            Product updated = productService.updateProduct(id, request);
            return ResponseEntity.ok(CommonResponseDTO.ok(updated, "Product updated"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CommonResponseDTO.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product",
               description = "Removes entry from GemFire region and evicts from Spring cache.")
    public ResponseEntity<CommonResponseDTO<Void>> delete(@PathVariable String id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(CommonResponseDTO.ok(null, "Product deleted"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CommonResponseDTO.error(e.getMessage()));
        }
    }

    // ── OQL Queries ─────────────────────────────────────────────────────────────

    @GetMapping("/category/{category}")
    @Operation(summary = "Query by category",
               description = "GemFire Feature: OQL — SELECT * FROM /Products WHERE category = $1")
    public ResponseEntity<CommonResponseDTO<List<Product>>> byCategory(@PathVariable String category) {
        return ResponseEntity.ok(CommonResponseDTO.ok(productService.findByCategory(category), "Fetched the details"));
    }

    @GetMapping("/price-range")
    @Operation(summary = "Query by price range",
               description = "GemFire OQL: range query with ORDER BY. Params: min, max (in cents)")
    public ResponseEntity<CommonResponseDTO<List<Product>>> byPriceRange(
            @RequestParam int min,
            @RequestParam int max) {
        return ResponseEntity.ok(CommonResponseDTO.ok(productService.findByPriceRange(min, max), "Fetched the details"));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active in-stock products",
               description = "GemFire OQL: boolean filter — WHERE active = true AND inStock = true")
    public ResponseEntity<CommonResponseDTO<List<Product>>> active() {
        return ResponseEntity.ok(CommonResponseDTO.ok(productService.findActiveProducts(), "Fetched the details"));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products",
               description = "GemFire OQL: filter + ORDER BY rating")
    public ResponseEntity<CommonResponseDTO<List<Product>>> featured() {
        return ResponseEntity.ok(CommonResponseDTO.ok(productService.findFeaturedProducts(), "Fetched the details"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products",
               description = "GemFire OQL: name/description keyword search")
    public ResponseEntity<CommonResponseDTO<List<Product>>> search(
            @RequestParam @Parameter(description = "Search keyword") String q) {
        return ResponseEntity.ok(CommonResponseDTO.ok(productService.search(q), "Fetched the details"));
    }

    // ── GemFire Function Execution ─────────────────────────────────────────────

    @PostMapping("/adjust-price")
    @Operation(summary = "Server-side price adjustment",
               description = "GemFire Feature: Function Execution — business logic runs WHERE data lives. " +
                             "Adjusts all products in a category by a percentage without pulling data to client.")
    public ResponseEntity<CommonResponseDTO<Map<String, Object>>> adjustPrice(
            @RequestParam String category,
            @RequestParam double adjustmentPct) {
        List<String> updatedIds = productService.adjustPricesByCategory(category, adjustmentPct);
        Map<String, Object> result = Map.of(
                "category", category,
                "adjustmentPct", adjustmentPct,
                "updatedProductIds", updatedIds,
                "count", updatedIds.size()
        );
        return ResponseEntity.ok(CommonResponseDTO.ok(result, "Price adjustment executed via GemFire Function"));
    }

    // ── Monitoring & Admin ─────────────────────────────────────────────────────

    @GetMapping("/admin/stats")
    @Operation(summary = "Region statistics",
               description = "GemFire Feature: Region metrics — size, data policy, scope")
    public ResponseEntity<CommonResponseDTO<Map<String, Object>>> stats() {
        return ResponseEntity.ok(CommonResponseDTO.ok(productService.getRegionStats(), "Fetched the details"));
    }

    @GetMapping("/admin/cluster")
    @Operation(summary = "Cluster info",
               description = "GemFire cluster member and region overview")
    public ResponseEntity<CommonResponseDTO<Map<String, Object>>> clusterInfo() {
        return ResponseEntity.ok(CommonResponseDTO.ok(productService.getClusterInfo(), "Fetched the details"));
    }

    @PostMapping("/admin/seed")
    @Operation(summary = "Seed sample data",
               description = "GemFire Feature: Bulk putAll — inserts sample products in one network hop")
    public ResponseEntity<CommonResponseDTO<String>> seed() {
        int count = productService.seedSampleData();
        return ResponseEntity.ok(CommonResponseDTO.ok(null, "Seeded " + count + " products via GemFire putAll"));
    }
}
