package com.gemfire.demo.config;

import com.gemfire.demo.model.Product;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.RegionEvent;
import org.apache.geode.cache.util.CacheListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * GemFire Cache Listener — Pub/Sub Event Notification.
 *
 * <p><strong>GemFire Feature: Reliable Event Notifications</strong>
 *
 * <p>CacheListeners receive synchronous callbacks for region and entry events.
 * This implements the Observer pattern at the data-grid level.
 *
 * <p>Events covered:
 * <ul>
 *   <li>afterCreate — new entry added</li>
 *   <li>afterUpdate — existing entry modified</li>
 *   <li>afterDestroy — entry removed</li>
 *   <li>afterInvalidate — entry TTL expired</li>
 *   <li>afterRegionClear — region flushed</li>
 * </ul>
 */
@Component
public class ProductCacheListener extends CacheListenerAdapter<String, Product> {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheListener.class);

    @Override
    public void afterCreate(EntryEvent<String, Product> event) {
        Product product = event.getNewValue();
        log.info("📥 [Cache Event] CREATED | Key={} | Product={} | Active={} | InStock={}",
                event.getKey(),
                product != null ? product.getName() : "null",
                product != null ? product.getActive() : "null",
                product != null ? product.getInStock() : "null");
    }

    @Override
    public void afterUpdate(EntryEvent<String, Product> event) {
        Product oldVal = event.getOldValue();
        Product newVal = event.getNewValue();
        log.info("✏️ [Cache Event] UPDATED | Key={} | OldPrice={} | NewPrice={}",
                event.getKey(),
                oldVal != null ? oldVal.getPrice() : "null",
                newVal != null ? newVal.getPrice() : "null");
    }

    @Override
    public void afterDestroy(EntryEvent<String, Product> event) {
        log.info("🗑️ [Cache Event] DELETED | Key={}", event.getKey());
    }

    @Override
    public void afterInvalidate(EntryEvent<String, Product> event) {
        // Fires when TTL expires (INVALIDATE action)
        log.info("⏰ [Cache Event] TTL EXPIRED (INVALIDATED) | Key={}", event.getKey());
    }

    @Override
    public void afterRegionClear(RegionEvent<String, Product> event) {
        log.warn("🧹 [Cache Event] REGION CLEARED | Region={}", event.getRegion().getName());
    }
}
