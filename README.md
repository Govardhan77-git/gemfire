# 🚀 Spring Boot + Broadcom GemFire In-Memory Demo

A production-grade demonstration of **Broadcom Tanzu GemFire** integrated with **Spring Boot 3.x**, showcasing core in-memory data grid features, containerized deployment, and enterprise-grade coding standards.

---

## 📋 Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [GemFire Features Demonstrated](#gemfire-features-demonstrated)
3. [Project Structure](#project-structure)
4. [Running with Docker (All Containers)](#running-with-docker)
5. [API Reference](#api-reference)
6. [NULL / Empty Value Handling](#null--empty-value-handling)
7. [Coding Standards & Engineering Principles](#coding-standards--engineering-principles)
8. [Demo Walkthrough](#demo-walkthrough)

---

## 🏗 Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                   Docker Network                         │
│                                                         │
│  ┌───────────────┐     ┌───────────────────────────┐    │
│  │ Spring Boot   │────▶│    GemFire Locator        │    │
│  │ App :8080     │     │    (Discovery + LB) :10334│    │
│  └───────────────┘     └───────────┬───────────────┘    │
│                                    │                     │
│                        ┌───────────▼───────────────┐    │
│                        │                           │    │
│              ┌─────────▼──────┐  ┌─────────▼──────┐│   │
│              │ GemFire Server1│  │ GemFire Server2││   │
│              │ :40404         │  │ :40405         ││   │
│              │ PARTITION data │  │ REDUNDANT copy ││   │
│              └────────────────┘  └────────────────┘│   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## ⚡ GemFire Features Demonstrated

| Feature | Implementation | Location |
|---------|----------------|----------|
| **REPLICATE Region** | Full data copy on all nodes — ideal for read-heavy reference data | `GemFireConfig.productsRegion()` |
| **PARTITION Region** | Sharded data — ideal for large write-heavy datasets | `GemFireConfig.ordersRegion()` |
| **OQL Queries** | SQL-like Object Query Language for in-memory objects | `ProductRepository.findBy*()` |
| **Continuous Querying (CQ)** | Push-based real-time event notifications on data changes | `ProductContinuousQueryListener` |
| **Cache Listener (Pub/Sub)** | Synchronous callbacks on every region event | `ProductCacheListener` |
| **Function Execution** | Server-side compute — logic runs WHERE data lives | `PriceAdjustmentFunction` |
| **TTL Expiration** | Automatic eviction of stale entries (idle timeout) | `GemFireConfig` expiration config |
| **PDX Serialization** | Cross-language portable data exchange | `Product.toData() / fromData()` |
| **Bulk putAll** | Single network hop for multi-entry inserts | `ProductRepository.saveAll()` |
| **Spring Cache Integration** | `@Cacheable` / `@CacheEvict` backed by GemFire | `ProductService` annotations |
| **Locator Discovery** | Dynamic server discovery and client routing | `docker-compose.yml` |
| **Region Statistics** | Live monitoring of region size, policy, scope | `/api/v1/products/admin/stats` |
| **High Availability** | Two servers → redundant partition copies | `docker-compose.yml` (server1 + server2) |

---

## 📁 Project Structure

```
gemfire-springboot/
├── Dockerfile                          # Multi-stage build (builder + runtime)
├── docker-compose.yml                  # Full stack: Locator + 2 Servers + App
├── pom.xml
└── src/
    ├── main/java/com/gemfire/demo/
    │   ├── GemFireDemoApplication.java  # Entry point
    │   ├── config/
    │   │   ├── GemFireConfig.java       # Region defs, PDX, TTL, cache manager
    │   │   ├── ProductCacheListener.java # Pub/Sub event callbacks
    │   │   ├── ProductContinuousQueryListener.java # Real-time CQ notifications
    │   │   └── PriceAdjustmentFunction.java # Server-side Function execution
    │   ├── controller/
    │   │   └── ProductController.java   # REST API (CRUD + OQL + Function + Admin)
    │   ├── exception/
    │   │   └── GlobalExceptionHandler.java # Centralized error handling
    │   ├── model/
    │   │   ├── Product.java             # Domain model with PDX serialization
    │   │   ├── ProductRequest.java      # DTO with null/empty handling + resolveDefaults()
    │   │   └── ApiResponse.java         # Uniform response envelope
    │   ├── repository/
    │   │   └── ProductRepository.java   # GemfireTemplate + OQL queries
    │   ├── service/
    │   │   └── ProductService.java      # Business logic + cache annotations
    │   └── util/
    │       └── NullSafeUtil.java        # Centralized null handling for int/boolean
    └── test/java/com/gemfire/demo/
        └── NullHandlingTest.java        # Unit tests for null/empty handling
```

---

## 🐳 Running with Docker

### Prerequisites
- Docker Desktop / Docker Engine 24+
- Docker Compose v2

### Start the full stack

```bash
# Build and start all containers
docker-compose up --build

# Or in detached mode
docker-compose up --build -d

# View logs
docker-compose logs -f app
docker-compose logs -f gemfire-locator
```

### Access points

| Service | URL |
|---------|-----|
| Spring Boot API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health Check | http://localhost:8080/actuator/health |
| GemFire Pulse | http://localhost:7070/pulse |

### Stop

```bash
docker-compose down -v    # -v removes volumes too
```

---

## 🌐 API Reference

### CRUD
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/products` | Create product (null handling applied) |
| `GET` | `/api/v1/products/{id}` | Get by ID (Spring Cache + GemFire) |
| `GET` | `/api/v1/products` | List all from GemFire region |
| `PATCH` | `/api/v1/products/{id}` | Partial update (null-safe merge) |
| `DELETE` | `/api/v1/products/{id}` | Remove + cache evict |

### OQL Queries
| Method | Endpoint | GemFire OQL |
|--------|----------|-------------|
| `GET` | `/api/v1/products/category/{cat}` | `WHERE category = $1` |
| `GET` | `/api/v1/products/price-range?min=&max=` | `WHERE price BETWEEN $1 AND $2 ORDER BY price` |
| `GET` | `/api/v1/products/active` | `WHERE active = true AND inStock = true` |
| `GET` | `/api/v1/products/featured` | `WHERE featured = true ORDER BY rating DESC` |
| `GET` | `/api/v1/products/search?q=` | `WHERE name LIKE %$1%` |

### GemFire Features
| Method | Endpoint | Feature |
|--------|----------|---------|
| `POST` | `/api/v1/products/adjust-price?category=&adjustmentPct=` | **Function Execution** |
| `GET` | `/api/v1/products/admin/stats` | Region statistics |
| `GET` | `/api/v1/products/admin/cluster` | Cluster topology |
| `POST` | `/api/v1/products/admin/seed` | Bulk **putAll** |

### Example: Create with null fields

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Widget Pro",
    "category": "Electronics",
    "price": null,
    "active": null,
    "inStock": null,
    "stockCount": null
  }'
```

**Result:** price → 0, active → true, inStock → false (0 stock), featured → false

---

## 🛡 NULL / Empty Value Handling

### The Problem
When a client sends `"price": null` or omits a field entirely, Java primitives (`int`, `boolean`) would cause:
- `int price` → auto-unbox of null = `NullPointerException`
- Jackson defaults `int` to `0` even for missing fields (hiding the null intent)

### The Solution

**1. Use boxed types in DTOs**
```java
// ❌ Primitive — cannot represent null
private int price;
private boolean active;

// ✅ Boxed — null is explicit and preserved
private Integer price;   // null = "not provided"
private Boolean active;  // null = "not provided"
```

**2. Apply defaults in one place**
```java
// ProductRequest.resolveDefaults()
if (price == null)  price = 0;
if (active == null) active = Boolean.TRUE;   // new products active by default
if (inStock == null) inStock = stockCount > 0; // derived from stock
```

**3. NullSafeUtil — DRY helper**
```java
NullSafeUtil.defaultInt(request.getPrice(), 0)      // null → 0
NullSafeUtil.defaultBool(request.getActive(), true) // null → true
NullSafeUtil.nonNegativeInt(request.getStockCount())// null or -1 → 0
```

**4. PDX Serialization preserves null**
```java
// Uses writeObject (not writeInt) to preserve null in GemFire
writer.writeObject("price", price);    // null safe
writer.writeObject("active", active);  // null safe
```

---

## 🏆 Coding Standards & Engineering Principles

| Principle | Implementation |
|-----------|----------------|
| **Single Responsibility (SRP)** | Controller → routing only. Service → business logic. Repository → GemFire I/O. Config → infrastructure. |
| **Open/Closed (OCP)** | New region types added without modifying existing beans. New query methods without touching service. |
| **Dependency Inversion (DIP)** | Service depends on `ProductRepository` interface, not GemFire directly. |
| **DRY** | `NullSafeUtil` centralizes all null-to-default logic. `ApiResponse` centralizes all response shaping. |
| **Fail Fast** | `ProductRequest.validate()` checks required fields before any I/O. |
| **Immutability** | `Product` built via `@Builder` — no partial construction. |
| **Encapsulation** | GemFire region access hidden behind repository. Controllers never touch `GemfireTemplate`. |
| **Logging** | Structured logging at appropriate levels (INFO for state changes, WARN for TTL, ERROR for exceptions). |
| **Uniform Contract** | All endpoints return `ApiResponse<T>` envelope with `success`, `data`, `error`, `timestamp`. |
| **Layered Architecture** | Controller → Service → Repository → GemFire (strict unidirectional). |
| **Containerization** | Multi-stage Dockerfile, non-root user, container memory tuning, health checks. |
| **Documentation** | Javadoc on all public types and methods. Swagger annotations on all endpoints. |

---

## 🎬 Demo Walkthrough

1. **Start containers** — `docker-compose up --build`
2. **Open Swagger UI** — http://localhost:8080/swagger-ui.html
3. **Seed data** — `POST /api/v1/products/admin/seed` (demonstrates GemFire `putAll`)
4. **OQL query** — `GET /api/v1/products/category/Electronics`
5. **Create with nulls** — `POST /api/v1/products` with `"price": null, "active": null`
6. **Check logs** — observe CQ and CacheListener events fire in real-time
7. **Function execution** — `POST /api/v1/products/adjust-price?category=Electronics&adjustmentPct=10`
8. **Region stats** — `GET /api/v1/products/admin/stats`
9. **Cluster info** — `GET /api/v1/products/admin/cluster`
10. **GemFire Pulse** — http://localhost:7070/pulse (region visualization)
