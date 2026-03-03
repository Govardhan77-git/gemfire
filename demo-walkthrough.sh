#!/bin/bash
# =============================================================================
# GemFire gfsh Demo Script — Video Walkthrough
# Run this after: docker-compose up --build
#
# gfsh is the GemFire Shell — the primary CLI tool for managing,
# monitoring, and querying a GemFire cluster.
# =============================================================================

LOCATOR="gemfire-locator[10334]"
APP_URL="http://localhost:8080/api/v1"

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║       BROADCOM GEMFIRE + SPRING BOOT DEMO WALKTHROUGH       ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# ── STEP 1: Connect to the cluster via gfsh ──────────────────────────────────
echo "▶ STEP 1: Connect to GemFire Locator"
echo "  gfsh> connect --locator=$LOCATOR"
echo ""
echo "  The Locator handles member discovery and routes clients to the"
echo "  least-loaded server — this is GemFire's Single-Hop Capability."
echo ""

# ── STEP 2: List cluster members ─────────────────────────────────────────────
echo "▶ STEP 2: Inspect Cluster Members (Continuous Availability)"
echo "  gfsh> list members"
echo "  Expected: locator1 | server1 | server2 — 3 members in distributed system"
echo ""

# ── STEP 3: List regions ─────────────────────────────────────────────────────
echo "▶ STEP 3: List All GemFire Regions"
echo "  gfsh> list regions"
echo "  Expected: /Products (REPLICATE) | /Orders (PARTITION) | /Sessions (LOCAL) | /Audit (PERSISTENT)"
echo ""

# ── STEP 4: Describe each region (shows data policy, TTL, disk config) ───────
echo "▶ STEP 4: Describe Regions — Data Policies"
echo "  gfsh> describe region --name=/Products"
echo "       → REPLICATE | idle TTL=600s | CacheListener attached"
echo "  gfsh> describe region --name=/Orders"
echo "       → PARTITION | redundancy-copies=1 (HA across 2 servers)"
echo "  gfsh> describe region --name=/Sessions"
echo "       → LOCAL | absolute TTL=1800s"
echo "  gfsh> describe region --name=/Audit"
echo "       → REPLICATE_PERSISTENT | DiskStore=auditDiskStore | sync writes"
echo ""

# ── STEP 5: Seed data via API ─────────────────────────────────────────────────
echo "▶ STEP 5: Seed Sample Data (GemFire bulk putAll)"
echo "  curl -X POST $APP_URL/products/admin/seed"
echo "  → 5 products inserted in ONE network hop via GemFire putAll"
echo ""
curl -s -X POST "$APP_URL/products/admin/seed" 2>/dev/null && echo "" || echo "  (App may not be running — start with docker-compose up)"
echo ""

# ── STEP 6: OQL Query in gfsh ─────────────────────────────────────────────────
echo "▶ STEP 6: OQL Query — Object Query Language (gfsh)"
echo "  gfsh> query --query=\"SELECT * FROM /Products p WHERE p.active = true\""
echo "  gfsh> query --query=\"SELECT p.name, p.price FROM /Products p WHERE p.price > 100 ORDER BY p.price DESC\""
echo "  gfsh> query --query=\"SELECT p.category, COUNT(*) FROM /Products p GROUP BY p.category\""
echo ""

# ── STEP 7: OQL Query via Spring Boot API ────────────────────────────────────
echo "▶ STEP 7: OQL Queries via REST API"
echo "  GET $APP_URL/products/active"
echo "  GET $APP_URL/products/category/Electronics"
echo "  GET $APP_URL/products/price-range?min=10&max=500"
echo "  GET $APP_URL/products/search?q=laptop"
echo ""

# ── STEP 8: NULL handling demo ────────────────────────────────────────────────
echo "▶ STEP 8: NULL / Empty Handling for int and boolean"
echo "  Sending: { name, category, price: null, active: null, stockCount: null }"
echo ""
curl -s -X POST "$APP_URL/products" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Null Product","category":"Demo","price":null,"active":null,"stockCount":null,"featured":null}' \
  2>/dev/null | python3 -m json.tool 2>/dev/null || echo "  Payload: price=null→0, active=null→true, inStock=null→false(0 stock)"
echo ""
echo "  Result: price→0, active→true, featured→false, inStock→false"
echo "  Strategy: Boxed Integer/Boolean in DTO + resolveDefaults() + NullSafeUtil"
echo ""

# ── STEP 9: GemFire Function Execution ───────────────────────────────────────
echo "▶ STEP 9: Server-Side Function Execution"
echo "  POST $APP_URL/products/adjust-price?category=Electronics&adjustmentPct=10"
echo "  → PriceAdjustmentFunction runs WHERE DATA LIVES (no data transfer to client)"
echo ""
curl -s -X POST "$APP_URL/products/adjust-price?category=Electronics&adjustmentPct=10" \
  2>/dev/null | python3 -m json.tool 2>/dev/null || echo "  (start app first)"
echo ""

# ── STEP 10: GemFire Transactions ────────────────────────────────────────────
echo "▶ STEP 10: ACID Transaction — Cross-Region"
echo "  POST $APP_URL/gemfire/transactions/transfer-stock?fromId=ID1&toId=ID2&quantity=5"
echo "  → begin tx → debit Products + credit Products + write Audit → commit"
echo "  → If conflict: CommitConflictException → automatic rollback"
echo ""

# ── STEP 11: Disk Persistence ─────────────────────────────────────────────────
echo "▶ STEP 11: Shared-Nothing Disk Persistence"
echo "  POST $APP_URL/gemfire/audit?key=test-entry&value=persisted!"
echo "  GET  $APP_URL/gemfire/audit"
echo "  → Writes to /Audit REPLICATE_PERSISTENT region, flushed to disk synchronously"
echo ""
curl -s -X POST "$APP_URL/gemfire/audit?key=demo-key&value=I+survive+restarts" 2>/dev/null | python3 -m json.tool 2>/dev/null || echo "  (start app first)"
echo ""

# ── STEP 12: Continuous Querying — watch logs ─────────────────────────────────
echo "▶ STEP 12: Continuous Querying (CQ) — Real-Time Push Events"
echo "  Watch app logs while creating products:"
echo "  docker-compose logs -f app"
echo ""
echo "  Then POST a product with price > 100 → HighValueProductCQ fires"
echo "  Then POST a product with stockCount=0 → OutOfStockCQ fires"
echo "  Then POST featured=true → FeaturedProductCQ fires"
echo ""

# ── STEP 13: Region topology ──────────────────────────────────────────────────
echo "▶ STEP 13: Full Region Topology"
echo "  GET $APP_URL/gemfire/regions"
echo "  → Shows all 4 regions, data policies, TTL, disk config, feature mapping"
echo ""

# ── STEP 14: gfsh show metrics ────────────────────────────────────────────────
echo "▶ STEP 14: Cluster Statistics & Monitoring (gfsh)"
echo "  gfsh> show metrics"
echo "  gfsh> show metrics --member=server1"
echo "  gfsh> show metrics --region=/Products"
echo "  → GemFire Pulse: http://localhost:7070/pulse (visual dashboard)"
echo ""

# ── STEP 15: Swagger UI ───────────────────────────────────────────────────────
echo "▶ STEP 15: Swagger UI — All Endpoints"
echo "  http://localhost:8080/swagger-ui.html"
echo ""

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                    DEMO COMPLETE ✅                          ║"
echo "║                                                              ║"
echo "║  GemFire Features Covered:                                   ║"
echo "║  ✅ REPLICATE Region         ✅ OQL Queries                  ║"
echo "║  ✅ PARTITION Region         ✅ Continuous Querying (CQ)     ║"
echo "║  ✅ LOCAL / Tiered Cache     ✅ Cache Listener (Pub/Sub)     ║"
echo "║  ✅ Disk Persistence         ✅ Function Execution           ║"
echo "║  ✅ TTL Expiration           ✅ PDX Serialization            ║"
echo "║  ✅ ACID Transactions        ✅ Bulk putAll                  ║"
echo "║  ✅ Locator / Single-Hop     ✅ High Availability (2 servers)║"
echo "║  ✅ NULL/empty int+bool      ✅ Spring Cache Integration     ║"
echo "╚══════════════════════════════════════════════════════════════╝"
