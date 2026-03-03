package com.gemfire.demo.config;

import com.gemfire.demo.model.Product;
import org.apache.geode.cache.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.File;

/**
 * GemFire Cache and Region Configuration.
 *
 * GemFire Core Features from Broadcom docs:
 *   1. REPLICATE Region       - full data copy on every node (Products)
 *   2. PARTITION Region       - sharded data across nodes (Orders)
 *   3. LOCAL Region           - tiered caching in local memory (Sessions)
 *   4. Disk Persistence       - shared-nothing disk store (Audit)
 *   5. TTL Expiration         - idle timeout + absolute TTL
 *   6. PDX Serialization      - cross-language, schema-safe
 *   7. CacheListener          - pub/sub event notifications
 *   8. Statistics             - per-region sampling for monitoring
 */
@Configuration
public class GemFireCacheConfig {

    private static final Logger log = LoggerFactory.getLogger(GemFireCacheConfig.class);

    @Bean
    @Primary
    public Cache gemFireCache() {
        CacheFactory factory = new CacheFactory()
                .set("name", "GemFireDemoApp")
                .set("log-level", "warning")
                .set("statistic-sampling-enabled", "true")
                .setPdxReadSerialized(false)
                .setPdxIgnoreUnreadFields(true);
        Cache cache = factory.create();
        log.info("GemFire embedded peer Cache created");
        return cache;
    }

    /**
     * PRODUCTS — REPLICATE region.
     * GemFire Feature: High Read Throughput — every node has full data, zero network hops.
     * Also: CacheListener (pub/sub) + Idle TTL expiration.
     */
    @Bean("Products")
    public Region<String, Product> productsRegion(Cache cache, ProductCacheListener cacheListener) {
        RegionFactory<String, Product> rf = cache.createRegionFactory(RegionShortcut.REPLICATE);
        rf.setStatisticsEnabled(true);
        rf.addCacheListener(cacheListener);
        rf.setEntryIdleTimeout(new ExpirationAttributes(600, ExpirationAction.INVALIDATE));
        Region<String, Product> region = rf.create("Products");
        log.info("GemFire Region /Products [REPLICATE | idle TTL=600s | CacheListener]");
        return region;
    }

    /**
     * ORDERS — PARTITION region.
     * GemFire Feature: High Scalability — sharded buckets distributed across cluster nodes.
     * Pair with 2 GemFire servers in docker-compose for real redundant partitioning.
     */
    @Bean("Orders")
    public Region<String, Object> ordersRegion(Cache cache) {
        RegionFactory<String, Object> rf = cache.createRegionFactory(RegionShortcut.PARTITION);
        rf.setStatisticsEnabled(true);
        Region<String, Object> region = rf.create("Orders");
        log.info("GemFire Region /Orders [PARTITION | HA via 2-server cluster]");
        return region;
    }

    /**
     * SESSIONS — LOCAL region.
     * GemFire Feature: Tiered Caching / Reduced Cost of Ownership.
     * Hot data lives in local memory; absolute TTL destroys stale sessions automatically.
     */
    @Bean("Sessions")
    public Region<String, Object> sessionsRegion(Cache cache) {
        RegionFactory<String, Object> rf = cache.createRegionFactory(RegionShortcut.LOCAL);
        rf.setStatisticsEnabled(true);
        rf.setEntryTimeToLive(new ExpirationAttributes(1800, ExpirationAction.DESTROY));
        Region<String, Object> region = rf.create("Sessions");
        log.info("GemFire Region /Sessions [LOCAL | absolute TTL=1800s]");
        return region;
    }

    /**
     * AUDIT — REPLICATE_PERSISTENT region.
     *
     * GemFire Feature: Shared-Nothing Disk Persistence.
     * Each cluster member manages its OWN disk files independently — no shared storage.
     * If one member's disk fails, others are completely unaffected.
     * Data survives full cluster restarts (GemFire replays the oplog on startup).
     *
     * Write mode: SYNCHRONOUS — every put is flushed to disk before returning,
     * guaranteeing durability for audit/compliance data.
     */
    @Bean("Audit")
    public Region<String, Object> auditRegion(Cache cache) {
        File diskDir = new File("./gemfire-disk-store");
        diskDir.mkdirs();

        DiskStore diskStore = cache.findDiskStore("auditDiskStore");
        if (diskStore == null) {
            diskStore = cache.createDiskStoreFactory()
                    .setDiskDirs(new File[]{diskDir})
                    .setAutoCompact(true)
                    .setMaxOplogSize(512)
                    .create("auditDiskStore");
        }

        RegionFactory<String, Object> rf =
                cache.createRegionFactory(RegionShortcut.REPLICATE_PERSISTENT);
        rf.setStatisticsEnabled(true);
        rf.setDiskStoreName("auditDiskStore");
        rf.setDiskSynchronous(true);

        Region<String, Object> region = rf.create("Audit");
        log.info("GemFire Region /Audit [REPLICATE_PERSISTENT | DiskStore | SYNC writes]");
        return region;
    }
}
