package com.gemfire.demo.config;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * GemFire Continuous Query (CQ) setup using native Geode API.
 *
 * GemFire Feature: Continuous Querying
 * CQ fires real-time push events whenever data changes match the OQL predicate.
 *
 * Three CQs registered:
 *   1. HighValueProductCQ  — price > 100
 *   2. OutOfStockCQ        — inStock = false
 *   3. FeaturedProductCQ   — featured = true AND active = true
 */
@Configuration
public class ProductContinuousQueryListener {

    private static final Logger log = LoggerFactory.getLogger(ProductContinuousQueryListener.class);

    private final Cache cache;

    public ProductContinuousQueryListener(Cache cache) {
        this.cache = cache;
    }

    @PostConstruct
    public void registerContinuousQueries() {
        QueryService qs = cache.getQueryService();

        registerCq(qs, "HighValueProductCQ",
                "SELECT * FROM /Products p WHERE p.price > 100",
                new CqListener() {
                    @Override
                    public void onEvent(CqEvent event) {
                        log.info("🔔 [CQ - High Value] Op={} | Key={}", event.getQueryOperation(), event.getKey());
                    }
                    @Override
                    public void onError(CqEvent event) {
                        log.error("❌ [CQ - High Value] Error: {}", event.getThrowable().getMessage());
                    }
                    @Override
                    public void close() {
                        log.info("🔒 [CQ - High Value] Closed");
                    }
                });

        registerCq(qs, "OutOfStockCQ",
                "SELECT * FROM /Products p WHERE p.inStock = false",
                new CqListener() {
                    @Override
                    public void onEvent(CqEvent event) {
                        log.warn("⚠️ [CQ - Out of Stock] Key={}", event.getKey());
                    }
                    @Override
                    public void onError(CqEvent event) {
                        log.error("❌ [CQ - Out of Stock] Error: {}", event.getThrowable().getMessage());
                    }
                    @Override
                    public void close() {
                        log.info("🔒 [CQ - Out of Stock] Closed");
                    }
                });

        registerCq(qs, "FeaturedProductCQ",
                "SELECT * FROM /Products p WHERE p.featured = true AND p.active = true",
                new CqListener() {
                    @Override
                    public void onEvent(CqEvent event) {
                        log.info("⭐ [CQ - Featured] Op={} | Key={}", event.getQueryOperation(), event.getKey());
                    }
                    @Override
                    public void onError(CqEvent event) {
                        log.error("❌ [CQ - Featured] Error: {}", event.getThrowable().getMessage());
                    }
                    @Override
                    public void close() {
                        log.info("🔒 [CQ - Featured] Closed");
                    }
                });

        log.info("✅ Registered 3 GemFire Continuous Queries");
    }

    private void registerCq(QueryService qs, String name, String query, CqListener listener) {
        try {
            CqQuery existing = qs.getCq(name);
            if (existing != null) {
                existing.stop();
                existing.close();
            }
            CqAttributesFactory cqFactory = new CqAttributesFactory();
            cqFactory.addCqListener(listener);
            CqQuery cq = qs.newCq(name, query, cqFactory.create());
            cq.execute();
            log.info("  ✔ CQ registered: {}", name);
        } catch (Exception e) {
            log.warn("  ⚠ CQ '{}' skipped: {}", name, e.getMessage());
        }
    }
}
