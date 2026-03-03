package com.gemfire.demo.controller;

import com.gemfire.demo.model.CommonResponseDTO;
import com.gemfire.demo.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Controller demonstrating advanced GemFire core features:
 *   - ACID Transactions (cross-region commit/rollback)
 *   - Shared-Nothing Disk Persistence (Audit region)
 *   - Region Topology (all 4 regions with data policies)
 *   - Continuous Availability (HA cluster info)
 */
@RestController
@RequestMapping("/api/v1/gemfire")
@Tag(name = "GemFire Features", description = "Advanced GemFire capabilities demo")
public class GemFireFeaturesController {

    private static final Logger log = LoggerFactory.getLogger(GemFireFeaturesController.class);

    private final TransactionService transactionService;
    private final Cache cache;

    public GemFireFeaturesController(TransactionService transactionService, Cache cache) {
        this.transactionService = transactionService;
        this.cache = cache;
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    @PostMapping("/transactions/transfer-stock")
    @Operation(summary = "Stock transfer — GemFire ACID Transaction",
               description = "Atomically moves stock between two products AND writes an audit " +
                             "record to the persistent /Audit region — all in one commit. " +
                             "On conflict: CommitConflictException triggers automatic rollback.")
    public ResponseEntity<CommonResponseDTO<Map<String, Object>>> transferStock(
            @RequestParam String fromId,
            @RequestParam String toId,
            @RequestParam int quantity) {
        try {
            Map<String, Object> result = transactionService.transferStock(fromId, toId, quantity);
            return ResponseEntity.ok(CommonResponseDTO.ok(result, "Stock transfer committed atomically"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(CommonResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/transactions/bulk-activate")
    @Operation(summary = "Bulk activate/deactivate — GemFire Transaction",
               description = "All products in a category updated in ONE atomic transaction. " +
                             "If any put fails, ALL are rolled back.")
    public ResponseEntity<CommonResponseDTO<Map<String, Object>>> bulkActivate(
            @RequestParam String category,
            @RequestParam boolean active) {
        Map<String, Object> result = transactionService.bulkSetActiveByCategory(category, active);
        return ResponseEntity.ok(CommonResponseDTO.ok(result, "Bulk update committed"));
    }

    // ── Disk Persistence ─────────────────────────────────────────────────────

    @GetMapping("/audit")
    @Operation(summary = "Read Audit region — Shared-Nothing Disk Persistence",
               description = "/Audit is REPLICATE_PERSISTENT — written to disk synchronously. " +
                             "Survives cluster restarts. Each member owns its own disk files.")
    public ResponseEntity<CommonResponseDTO<Map<String, Object>>> getAuditEntries() {
        Region<String, Object> audit = cache.getRegion("Audit");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("regionName", audit.getName());
        result.put("dataPolicy", audit.getAttributes().getDataPolicy().toString());
        result.put("diskStoreName", audit.getAttributes().getDiskStoreName());
        result.put("diskSynchronous", audit.getAttributes().isDiskSynchronous());
        result.put("entryCount", audit.size());
        Map<String, Object> entries = new LinkedHashMap<>();
        audit.entrySet().forEach(e -> entries.put(e.getKey(), e.getValue()));
        result.put("entries", entries);
        return ResponseEntity.ok(CommonResponseDTO.ok(result, "Fetched the details"));
    }

    @PostMapping("/audit")
    @Operation(summary = "Write to Audit region (disk-persisted)")
    public ResponseEntity<CommonResponseDTO<Map<String, Object>>> writeAudit(
            @RequestParam String key,
            @RequestParam String value) {
        Region<String, Object> audit = cache.getRegion("Audit");
        audit.put(key, Map.of("value", value, "timestamp", Instant.now().toString()));
        return ResponseEntity.ok(CommonResponseDTO.ok(
                Map.of("key", key, "persisted", true, "diskStore", "auditDiskStore"),
                "Written to disk-persistent Audit region"));
    }

    // ── Region Topology ───────────────────────────────────────────────────────

    @GetMapping("/regions")
    @Operation(summary = "All GemFire regions — topology overview",
               description = "Shows all 4 regions with data policies, TTL, disk config, entry counts.")
    public ResponseEntity<CommonResponseDTO<Map<String, Object>>> getAllRegions() {
        Map<String, Object> topology = new LinkedHashMap<>();

        for (String name : List.of("Products", "Orders", "Sessions", "Audit")) {
            Region<?, ?> region = cache.getRegion(name);
            if (region != null) {
                RegionAttributes<?, ?> attrs = region.getAttributes();
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("dataPolicy", attrs.getDataPolicy().toString());
                info.put("scope", attrs.getScope().toString());
                info.put("entryCount", region.size());

                if (attrs.getEntryIdleTimeout() != null && attrs.getEntryIdleTimeout().getTimeout() > 0) {
                    info.put("idleTTL_seconds", attrs.getEntryIdleTimeout().getTimeout());
                    info.put("idleTTL_action", attrs.getEntryIdleTimeout().getAction().toString());
                }
                if (attrs.getEntryTimeToLive() != null && attrs.getEntryTimeToLive().getTimeout() > 0) {
                    info.put("absoluteTTL_seconds", attrs.getEntryTimeToLive().getTimeout());
                    info.put("absoluteTTL_action", attrs.getEntryTimeToLive().getAction().toString());
                }
                if (attrs.getDiskStoreName() != null) {
                    info.put("diskStore", attrs.getDiskStoreName());
                    info.put("diskSynchronous", attrs.isDiskSynchronous());
                }
                topology.put(name, info);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("clusterMember",
                cache.getDistributedSystem().getDistributedMember().getId());
        response.put("regions", topology);
        response.put("featuresImplemented", Map.ofEntries(
                Map.entry("REPLICATE",       "Products — full copy on every node, zero-hop reads"),
                Map.entry("PARTITION",       "Orders — sharded across nodes, horizontal scale"),
                Map.entry("LOCAL_TTL",       "Sessions — tiered cache, absolute TTL expiry"),
                Map.entry("PERSISTENT",      "Audit — shared-nothing disk, survives restarts"),
                Map.entry("CacheListener",   "Products — pub/sub afterCreate/afterUpdate/afterDestroy"),
                Map.entry("ContinuousQuery", "3 CQs — high-value, out-of-stock, featured"),
                Map.entry("FunctionExec",    "PriceAdjustmentFunction — server-side compute"),
                Map.entry("PDX",             "Product implements PdxSerializable — cross-language"),
                Map.entry("OQL",             "5 query types — WHERE, ORDER BY, range, boolean filters"),
                Map.entry("Transaction",     "ACID across Products + Audit regions"),
                Map.entry("BulkOps",         "putAll — single network hop for batch writes")
        ));

        return ResponseEntity.ok(CommonResponseDTO.ok(response, "Fetched the details"));
    }

    // ── Cluster Health ────────────────────────────────────────────────────────

    @GetMapping("/cluster/health")
    @Operation(summary = "Cluster health — Continuous Availability",
               description = "2 GemFire servers in docker-compose provide redundant partition " +
                             "copies. If server-1 dies, server-2 serves all requests without data loss.")
    public ResponseEntity<CommonResponseDTO<Map<String, Object>>> clusterHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("member", cache.getDistributedSystem().getDistributedMember().getId());
        health.put("cacheOpen", !cache.isClosed());
        health.put("totalRegions", cache.rootRegions().size());
        health.put("haStrategy", "2 GemFire servers — PARTITION redundancy + auto-failover");
        health.put("topology", Map.of(
                "locator", "gemfire-locator:10334 — member discovery + load balancing",
                "server1",  "gemfire-server-1:40404 — primary partition buckets",
                "server2",  "gemfire-server-2:40405 — redundant copies, auto-failover"
        ));
        return ResponseEntity.ok(CommonResponseDTO.ok(health, "Fetched the details"));
    }
}
