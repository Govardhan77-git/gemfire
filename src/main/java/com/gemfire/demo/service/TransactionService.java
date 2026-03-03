package com.gemfire.demo.service;

import com.gemfire.demo.model.Product;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheTransactionManager;
import org.apache.geode.cache.CommitConflictException;
import org.apache.geode.cache.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Demonstrates GemFire Transaction Management.
 *
 * <p><strong>GemFire Feature: ACID Transactions</strong>
 *
 * <p>GemFire supports distributed ACID transactions across multiple regions and entries.
 * All puts/removes inside a transaction either ALL commit or ALL rollback atomically.
 *
 * <p>Key characteristics:
 * <ul>
 *   <li>Optimistic locking — conflicts detected at commit time</li>
 *   <li>Cross-region — a single tx can span Products + Audit regions</li>
 *   <li>{@link CommitConflictException} thrown if concurrent updates conflict</li>
 *   <li>No distributed deadlocks — GemFire uses optimistic concurrency</li>
 * </ul>
 *
 * <p>Coding principle: This service is purely transactional logic (SRP).
 * It does not own region configuration or REST concerns.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final Cache cache;

    public TransactionService(Cache cache) {
        this.cache = cache;
    }

    /**
     * Atomically transfers stock between two products.
     *
     * <p>Both the debit and credit happen in a single GemFire transaction.
     * If either fails, both changes are rolled back — inventory is never inconsistent.
     *
     * @param fromProductId product to debit stock from
     * @param toProductId   product to credit stock to
     * @param quantity      units to transfer
     * @return summary of the transaction result
     */
    public Map<String, Object> transferStock(String fromProductId,
                                              String toProductId,
                                              int quantity) {
        Region<String, Product> products = cache.getRegion("Products");
        Region<String, Object> audit = cache.getRegion("Audit");
        CacheTransactionManager txManager = cache.getCacheTransactionManager();

        txManager.begin();
        log.info("[Transaction] BEGIN — transferring {} units from {} to {}", quantity, fromProductId, toProductId);

        try {
            // ── Read both products inside the transaction ─────────────────────
            Product source = products.get(fromProductId);
            Product target = products.get(toProductId);

            if (source == null) throw new IllegalArgumentException("Source product not found: " + fromProductId);
            if (target == null) throw new IllegalArgumentException("Target product not found: " + toProductId);

            int sourceStock = source.getStockCount() != null ? source.getStockCount() : 0;
            if (sourceStock < quantity) {
                throw new IllegalStateException(
                        "Insufficient stock: " + source.getName() + " has " + sourceStock + ", need " + quantity);
            }

            // ── Apply changes inside the transaction ──────────────────────────
            source.setStockCount(sourceStock - quantity);
            source.setInStock(source.getStockCount() > 0);
            source.setUpdatedAt(Instant.now());

            int targetStock = target.getStockCount() != null ? target.getStockCount() : 0;
            target.setStockCount(targetStock + quantity);
            target.setInStock(true);
            target.setUpdatedAt(Instant.now());

            products.put(fromProductId, source);
            products.put(toProductId, target);

            // ── Write audit record to persistent region in SAME transaction ───
            String auditKey = "tx-" + System.currentTimeMillis();
            audit.put(auditKey, Map.of(
                    "type", "STOCK_TRANSFER",
                    "from", fromProductId,
                    "to", toProductId,
                    "quantity", quantity,
                    "timestamp", Instant.now().toString()
            ));

            // ── Commit — all changes go live atomically ───────────────────────
            txManager.commit();
            log.info("[Transaction] COMMIT — transfer complete");

            return Map.of(
                    "status", "COMMITTED",
                    "from", source.getName(),
                    "fromStockAfter", source.getStockCount(),
                    "to", target.getName(),
                    "toStockAfter", target.getStockCount(),
                    "quantity", quantity,
                    "auditKey", auditKey
            );

        } catch (CommitConflictException cce) {
            // Optimistic concurrency conflict — another thread updated same entries
            log.warn("[Transaction] CONFLICT — rolling back: {}", cce.getMessage());
            if (txManager.exists()) txManager.rollback();
            throw new IllegalStateException("Transaction conflict — please retry: " + cce.getMessage());

        } catch (Exception e) {
            log.error("[Transaction] ERROR — rolling back: {}", e.getMessage());
            if (txManager.exists()) txManager.rollback();
            throw e;
        }
    }

    /**
     * Atomically bulk-activates or bulk-deactivates an entire product category.
     * All updates commit or rollback together.
     */
    public Map<String, Object> bulkSetActiveByCategory(String category, boolean active) {
        Region<String, Product> products = cache.getRegion("Products");
        CacheTransactionManager txManager = cache.getCacheTransactionManager();

        txManager.begin();
        log.info("[Transaction] BEGIN — bulk setActive={} for category={}", active, category);

        try {
            int count = 0;
            for (Map.Entry<String, Product> entry : products.entrySet()) {
                Product p = entry.getValue();
                if (p != null && category.equalsIgnoreCase(p.getCategory())) {
                    p.setActive(active);
                    p.setUpdatedAt(Instant.now());
                    products.put(entry.getKey(), p);
                    count++;
                }
            }
            txManager.commit();
            log.info("[Transaction] COMMIT — updated {} products", count);
            return Map.of("status", "COMMITTED", "category", category, "active", active, "updatedCount", count);

        } catch (Exception e) {
            log.error("[Transaction] ROLLBACK: {}", e.getMessage());
            if (txManager.exists()) txManager.rollback();
            throw e;
        }
    }
}
