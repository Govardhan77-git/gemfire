package com.gemfire.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the GemFire Spring Boot Demo Application.
 *
 * GemFire Features Demonstrated:
 *   - REPLICATE Region (Products)     - full copy on every node
 *   - PARTITION Region (Orders)       - sharded across nodes
 *   - LOCAL Region with TTL (Sessions)- tiered caching
 *   - Persistent Region (Audit)       - shared-nothing disk store
 *   - OQL Queries                     - 6 query types
 *   - Continuous Querying (CQ)        - real-time push events
 *   - Cache Listener (Pub/Sub)        - afterCreate/Update/Destroy
 *   - Function Execution              - server-side compute
 *   - ACID Transactions               - cross-region, commit/rollback
 *   - PDX Serialization               - cross-language portable format
 *   - TTL Expiration                  - idle + absolute
 *   - Bulk putAll                     - single-hop batch writes
 *   - NULL/empty int+boolean handling - boxed types + resolveDefaults()
 */
@SpringBootApplication
public class GemFireDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(GemFireDemoApplication.class, args);
    }
}
