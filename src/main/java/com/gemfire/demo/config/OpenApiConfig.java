package com.gemfire.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger UI configuration.
 *
 * <p>Provides a self-documenting API explorer at /swagger-ui.html
 * describing all GemFire features implemented in this demo.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gemFireDemoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Broadcom GemFire + Spring Boot Demo")
                        .version("1.0.0")
                        .description("""
                                ## Spring Boot + Broadcom Tanzu GemFire In-Memory Demo
                                
                                ### GemFire Core Features Implemented
                                
                                | Feature | Endpoint / Class |
                                |---------|-----------------|
                                | REPLICATE Region | GemFireCacheConfig → /Products |
                                | PARTITION Region | GemFireCacheConfig → /Orders |
                                | LOCAL Region + Tiered Cache | GemFireCacheConfig → /Sessions |
                                | Shared-Nothing Disk Persistence | GemFireCacheConfig → /Audit |
                                | OQL Queries | GET /api/v1/products/category, /price-range, /active, /featured, /search |
                                | Continuous Querying (CQ) | ProductContinuousQueryListener |
                                | Pub/Sub Cache Listener | ProductCacheListener |
                                | Server-Side Function Execution | POST /api/v1/products/adjust-price |
                                | ACID Transactions | POST /api/v1/gemfire/transactions/* |
                                | PDX Serialization | Product.toData() / fromData() |
                                | TTL Expiration | Products idle=600s, Sessions absolute=1800s |
                                | Bulk putAll | POST /api/v1/products/admin/seed |
                                | Spring Cache Integration | @Cacheable / @CacheEvict |
                                | Locator + Single-Hop | docker-compose gemfire-locator |
                                | High Availability | 2 GemFire servers with redundant copies |
                                | NULL/empty int+bool handling | ProductRequest.resolveDefaults() + NullSafeUtil |
                                
                                ### Run Everything on Containers
                                ```bash
                                docker-compose up --build
                                ```
                                
                                ### Demo Script
                                ```bash
                                chmod +x demo-walkthrough.sh && ./demo-walkthrough.sh
                                ```
                                """)
                        .contact(new Contact()
                                .name("GemFire Demo Team")
                                .email("demo@gemfire.com"))
                        .license(new License().name("Demo Use Only")))
                .tags(List.of(
                        new Tag().name("Products")
                                .description("CRUD + OQL queries on GemFire REPLICATE region"),
                        new Tag().name("GemFire Features")
                                .description("Transactions, Disk Persistence, Region Topology, HA")
                ));
    }
}
