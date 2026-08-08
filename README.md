 # NexusWMS

**A full-stack warehouse management system simulating a real e-commerce fulfillment center**. It covers supplier receiving, inventory storage, order picking, packing, and carrier dispatch. A live operations dashboard uses WebSocket and Redis pub/sub.

![Java](https://img.shields.io/badge/Java-21-red?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen?logo=springboot)
![Vue](https://img.shields.io/badge/Vue-3-42b883?logo=vuedotjs)
![FastAPI](https://img.shields.io/badge/FastAPI-Python%203.12-009688?logo=fastapi)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?logo=mongodb)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker)
![License](https://img.shields.io/badge/license-MIT-green)

> One warehouse. One distribution zone. Every package is traceable. Every action is logged. Every worker role is modeled. The system uses production‑grade engineering patterns across a polyglot stack. It is not a simplified portfolio demo.

---

## Jump to a section

<div align="center">

[**Overview**](#overview) · [**Screenshots**](#screenshots) · [**Architecture**](#architecture) · [**Tech Stack**](#tech-stack) · [**Features by Domain**](#features-by-domain) · [**Getting Started**](#getting-started) · [**API Reference**](#api-reference) · [**Testing**](#testing) · [**CI/CD**](#cicd) · [**Design System**](#design-system) · [**Engineering Decisions**](#engineering-decisions) · [**Roadmap**](#roadmap)

</div>

---

## Overview

NexusWMS manages the complete lifecycle of a physical order:

```
Supplier → Goods Receipt → Shelf Storage → Pick List → Packing → Carrier Dispatch
```

Eight role‑based actors use the system: Admin, Manager, Receiver, Picker, Packer, Dispatcher, Inventory Controller, and Finance. Each sees an interface that fits their job. Managers get data‑dense oversight dashboards. Workers use dark‑mode, single‑task terminals. All views run on the same real‑time event pipeline.

**Why this project exists:** It shows enterprise Java (Spring Boot 4), Python analytics (FastAPI + scikit‑learn), polyglot persistence (PostgreSQL + MongoDB + Redis), real‑time systems (WebSocket/STOMP + Redis pub/sub), and a real CI/CD pipeline. All these parts fit together in one system with a true business story, not in separate toy demos.

<details>
<summary><b>The golden path, end to end</b></summary>

```
1.  Manager creates an order                     → Order (RECEIVED)
2.  Manager generates a fulfillment request        → Order (VALIDATED)
3.  Manager generates a pick list                  → Order (PICKING), assigned to a Picker
4.  Picker scans each item off its shelf            → PickListItem (PICKED)
5.  Manager creates a packing task                 → assigned to a Packer
6.  Packer starts, then completes the task          → Order (PACKING) → Parcel created
7.  Dispatcher assigns a carrier                    → Order (DISPATCHED), Shipment closed
8.  Every step publishes to Redis pub/sub           → Live Ops dashboard updates in <1s
9.  Every step writes to MongoDB                    → full audit trail, queryable by tracking number
```

</details>

---

## Screenshots

> 🚧 **Screenshots pending** — the UI is fully built and functional (see [Features by Domain](#features-by-domain)); this section will be filled in with real captures of the Live Ops flow rail, worker terminals, and manager pillars.

<details>
<summary><b>Placeholder layout (click to see planned shots)</b></summary>

| Live Ops Dashboard | Worker Terminal (Picker) |
|---|---|
| `screenshots/live-ops.png` — flow rail, event feed, stat cards | `screenshots/pick-terminal.png` — dark single-task view |

| Procurement Pillar | Inventory Pillar |
|---|---|
| `screenshots/procurement.png` — suppliers, POs, goods receipts | `screenshots/inventory.png` — SKUs, stock, zones |

</details>

---

## Architecture

```
                              ┌──────────────────────┐
                              │   Vue 3 + TypeScript   │
                              │   Manager Shell /      │
                              │   Worker Terminals      │
                              │   Port: 5173            │
                              └───────────┬─────────────┘
                                          │ REST + STOMP/WebSocket
                     ┌────────────────────┼────────────────────┐
                     ▼                                          ▼
         ┌───────────────────────┐                  ┌──────────────────────┐
         │   Spring Boot 4.1.0    │                  │   FastAPI Analytics   │
         │   Core Service          │ ◄── shared JWT ──│   Port: 8001           │
         │   Auth · OMS · WMS ·    │      secret       │   Reports · Forecast  │
         │   IMS · PMS · WebSocket │                  │   (read-only PG)       │
         │   Port: 8080             │                  └──────────────────────┘
         └────────────┬──────────────┘
                       │
      ┌────────────────┼────────────────┐
      ▼                ▼                ▼
┌───────────┐   ┌────────────┐   ┌───────────┐
│ PostgreSQL │   │  MongoDB    │   │   Redis    │
│  Port 5432 │   │  Port 27017 │   │  Port 6379 │
│            │   │             │   │            │
│ Source of  │   │ Audit logs  │   │ JWT        │
│  truth,    │   │ Scan events │   │ blocklist, │
│ ACID txns  │   │ Order events│   │ pub/sub,   │
└───────────┘   └────────────┘   │ live cache  │
                                    └───────────┘
```

<details>
<summary><b>Bounded contexts (domain breakdown)</b></summary>

| Context | Owns |
|---|---|
| **OMS** — Order Management | Order intake, validation, status tracking, fulfillment requests |
| **WMS** — Warehouse Management | Inbound receiving, pick lists, pack confirmation, dispatch, scan logging |
| **IMS** — Inventory Management | SKU catalog, categories, zone/aisle/shelf locations, stock levels, reorder alerts |
| **PMS** — Procurement | Suppliers, purchase orders, goods receipts, 3-way invoice matching |
| **Live Operations** | Real-time dashboard — throughput, zone occupancy, active workers, bottleneck alerts |
| **Analytics** (FastAPI) | Worker performance, stock valuation, demand forecasting (scikit-learn) |

Cross-context communication rule, enforced throughout: **same package → JPA relationships allowed. Cross-package → UUID fields + service-layer calls only, never direct repository access.** DTOs are the one thing allowed to cross a package boundary (e.g. `UserSummaryResponse`, `SkuResponse`) — they're plain data contracts, not persistence types.

</details>

<details>
<summary><b>Database responsibilities</b></summary>

**PostgreSQL 16** : source of truth for everything transactional: users, SKUs, zones/aisles/shelves, orders, pick lists, packing tasks, parcels, shipments, purchase orders, goods receipts, invoices. UUID primary keys, VARCHAR enums (no `ALTER TYPE` friction), JSONB for flexible fields (SKU dimensions, customer address), Flyway-only schema changes.

**MongoDB 7** : high-volume append-only audit trail where schema genuinely varies: `scan_logs`, `order_events`, `PackingTaskEvent`. A receiving scan and a dispatch scan have different fields; forcing that into nullable Postgres columns would be worse than a flexible document.

**Redis 7** : JWT blocklist (`blocklist:user:{userId}`, TTL = remaining token lifetime, gives immediate revocation with zero maintenance job), live dashboard cache, and pub/sub channels (`warehouse.events`, `alerts`) that the Live Ops dashboard subscribes to.

</details>

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Core Backend** | Java 21, Spring Boot 4.1.0, Spring Security, Spring Data JPA, JJWT 0.12.6 |
| **Analytics Service** | Python 3.12, FastAPI, SQLAlchemy 2.0 async, scikit-learn, pandas |
| **Frontend** | Vue 3, TypeScript, Vite, Pinia, Vue Router, Zod, SCSS |
| **Primary DB** | PostgreSQL 16 (stored procedures, views, triggers, window functions) |
| **Document Store** | MongoDB 7 (audit logs, scan events, lifecycle events) |
| **Cache / Pub-Sub** | Redis 7 (JWT blocklist, WebSocket pub/sub) |
| **Real-time** | WebSocket via STOMP, SockJS fallback, frame-header JWT auth |
| **Auth** | JWT (JJWT) + BCrypt + Redis-backed revocation |
| **AWS Simulation** | LocalStack (region/banking restrictions in Tunisia block a real AWS account) |
| **Migrations** | Flyway (versioned SQL, 8 migrations applied) |
| **Containers** | Docker, Docker Compose |
| **CI/CD** | GitHub Actions — Checkstyle, ruff, ESLint, JUnit, Pytest, Vitest, Trivy, GHCR |
| **Testing** | JUnit 5 + Mockito, Pytest, Vitest + @vue/test-utils |

---

## Features by Domain

<details open>
<summary><b>🟣 Live Operations</b></summary>

- **Flow Rail** : a live cross-section of the five physical zones (Receiving → Storage → Picking → Packing → Dispatch). Moving dots represent goods in transit, worker-presence chips show zone occupancy, and a fill-strip crossing 80% pulses a bottleneck warning — all driven by a real WebSocket pipeline, not polling.
- STOMP-over-WebSocket, authenticated via CONNECT-frame JWT headers (migrated off URL query params to match Spring's documented pattern and avoid tokens leaking into proxy logs).
- App-shell-level socket connection : survives navigation between manager pages without reconnecting.

</details>

<details>
<summary><b>🔵 Procurement</b></summary>

- Supplier CRUD with status lifecycle (`ACTIVE` / `INACTIVE` / `BLACKLISTED`)
- Purchase order lifecycle: draft → approved → partially/fully received, with line-item tracking
- Goods receipt workflow that updates shelf stock and writes a `StockMovement` audit record on every receipt
- 3-way match (PO total = received value = invoice amount) with `MATCHED` / `DISCREPANCY` outcomes

</details>

<details>
<summary><b>⚪ Inventory</b></summary>

- Hierarchical category tree (self-referential FK, unlimited depth)
- Zone → Aisle → Shelf physical structure, with aisle/shelf codes correctly scoped per-parent (not globally unique — a real warehouse-naming bug found and fixed, see [Engineering Decisions](#engineering-decisions))
- Signed-quantity stock adjustment (positive = restock, negative = shrinkage/damage), zero rejected, cannot go below zero or below reserved quantity
- Automatic reorder alerts that **open and resolve** correctly as stock crosses the threshold in either direction
- Full stock movement audit trail

</details>

<details>
<summary><b>🟠 Fulfillment</b></summary>

- Order → fulfillment request → pick list → packing task → dispatch, fully modeled with status transitions at every stage
- Pick-list generation resolves the first available shelf location per SKU automatically
- Manager oversight views for Orders, Pick Lists, Packing, and Dispatch — separate from the worker terminals below
- Parcel tracking numbers, barcodes, and carrier tracking numbers auto-generated with bounded, collision-safe retry logic

</details>

<details>
<summary><b>🖥️ Worker Terminals (dark, single-task, tablet-oriented)</b></summary>

Real workers use lightweight tablets, not laptops — these are a deliberately separate route branch (`/terminal/*`) from the manager shell, with no pillar navigation:

- **Picker** : one item at a time, large monospace shelf-location readout, scan-to-confirm
- **Packer** : start/complete task state machine, weight capture on completion
- **Dispatcher** : shared queue view, per-parcel carrier assignment
- **Receiver** : select an approved PO, log received quantity/batch/shelf per line

</details>

<details>
<summary><b>🟢 Reports & Analytics (FastAPI, port 8001)</b></summary>

- Worker performance and stock valuation for a given day
- Demand forecasting via scikit-learn `LinearRegression` on stock movement history — real single-SKU sparkline output (`trend_slope`, `forecast[]` series, low-confidence flag under 5 data points)
- Deliberately **excludes** anything Live Ops already shows live (orders-today, active-workers) to avoid a slower-latency duplicate of the same data
- Shares the Spring Boot JWT secret and independently re-implements the Redis blocklist check — a suspended user is locked out of both services, not just one

</details>

<details>
<summary><b>🩷 Finance & 🧑‍🤝‍🧑 Employees</b></summary>

- **Finance** : invoice queue with 3-way match trigger
- **Employees** : roster with role/status filtering; role/status edits gated to ADMIN viewers only, and another admin's row is locked from casual inline editing

</details>

---

## Getting Started

<details>
<summary><b>Prerequisites</b></summary>

- Docker Desktop
- Java 21 (Temurin) + Maven
- Node.js 20+
- Python 3.12

</details>

### Local development

```bash
git clone https://github.com/Iskandar-Mhadhbi/NexusWMS.git
cd NexusWMS

# 1. Start the databases
docker-compose up postgres redis mongo -d

# 2. Backend (Spring Boot)
cd backend
mvn spring-boot:run
# → http://localhost:8080

# 3. Analytics service (FastAPI)
cd ../analytics
python run.py
# → http://localhost:8001

# 4. Frontend (Vue 3)
cd ../frontend
npm install
npm run dev
# → http://localhost:5173
```

<details>
<summary><b>Environment configuration</b></summary>

```bash
# Backend
cp backend/src/main/resources/application-dev.yml.example backend/src/main/resources/application-dev.yml

# Analytics
cp analytics/.env.example analytics/.env
# fill in DB password and a JWT secret — must match the Spring Boot secret exactly
```

Frontend `.env`:
```
VITE_API_BASE_URL=http://localhost:8080/api/v1.0
VITE_WS_BASE_URL=http://localhost:8080
VITE_ANALYTICS_BASE_URL=http://localhost:8001/api/v1/analytics
```

</details>

<details>
<summary><b>Test accounts</b></summary>

| Role | Email | Employee ID | Password |
|---|---|---|---|
| ADMIN | `test2@nexuswms.com` | `EMP-0002` | `password123` |

New registrations start as `PENDING` and require admin activation — this reflects a real HR onboarding flow, not a bug.

</details>

---

## API Reference

Swagger UI: `http://localhost:8080/swagger-ui.html` (disabled in production — see [Engineering Decisions](#engineering-decisions))
FastAPI docs: `http://localhost:8001/docs`

<details>
<summary><b>Full endpoint list</b></summary>

**Auth & Users**
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/logout
GET    /api/v1/users/me
GET    /api/v1/users
PATCH  /api/v1/users/{employeeId}/status
PATCH  /api/v1/users/{employeeId}/role
```

**Inventory**
```
GET/POST  /api/v1/zones
GET/POST  /api/v1/zones/aisles
GET/POST  /api/v1/zones/shelves
GET/POST  /api/v1/skus
GET/POST  /api/v1/skus/categories
GET       /api/v1/stock
POST      /api/v1/stock/adjust
GET       /api/v1/stock/alerts/reorder
GET       /api/v1/stock/{skuId}/movements
```

**Procurement**
```
GET/POST  /api/v1/suppliers
GET/POST  /api/v1/purchase-orders
POST      /api/v1/purchase-orders/{id}/approve
GET/POST  /api/v1/goods-receipts
GET/POST  /api/v1/invoices
POST      /api/v1/invoices/{id}/match
```

**Fulfillment**
```
GET/POST  /api/v1/orders
POST      /api/v1/orders/{id}/cancel
POST      /api/v1/orders/{id}/fulfillment-request
GET/POST  /api/v1/pick-lists
POST      /api/v1/pick-lists/{id}/items/{itemId}/pick
GET/POST  /api/v1/packing-tasks
POST      /api/v1/packing-tasks/{id}/start
POST      /api/v1/packing-tasks/{id}/complete
POST      /api/v1/parcels/{id}/dispatch
GET       /api/v1/parcels/{id}/shipment
GET       /api/v1/shipments
POST      /api/v1/scan
GET/POST  /api/v1/carriers
```

**Analytics (FastAPI, port 8001)**
```
GET  /api/v1/analytics/worker/performance
GET  /api/v1/analytics/stock/valuation
GET  /api/v1/analytics/forecast/demand
GET  /api/v1/analytics/reports/daily
```

**WebSocket**
```
STOMP over /ws/dashboard — JWT via CONNECT frame Authorization header
Topics: /topic/dashboard, /topic/alerts
```

</details>

---

## Testing

Automated tests exist and pass on both stacks; broad coverage across every service is a known, explicitly tracked gap rather than a finished effort.

<details>
<summary><b>Backend — JUnit 5 + Mockito</b></summary>

| Service | Scope |
|---|---|
| `UserService` | Full — all 5 methods, including batch `getUserSummaries()` |
| `PackingService` | Full — create/start/complete task, list, worker-scoped list |
| `PickListService` | Scoped — list, worker-scoped list, get-by-id, pick-item |
| `StockService` | Scoped — adjustment + reorder-alert open/resolve branches |
| `PurchaseOrderService` | Scoped — create, approve |

```bash
cd backend
mvn test
```

Not yet covered: `OrderService`, `DispatchService`, `GoodsReceiptService`, `PickListService.generate()`, auth package, FastAPI service.

</details>

<details>
<summary><b>Frontend — Vitest + @vue/test-utils</b></summary>

| File | Scope |
|---|---|
| `pickListOversightStore` | Full — initial state, success, filters, error/re-throw, loading states |
| `packingOversightStore` | Full — same shape as above |
| `PickListsView.vue` | Full — 8 tests, including a regression test for a real unhandled-promise-rejection bug found during this pass |

```bash
cd frontend
npx vitest run
```

Not yet covered: dispatch oversight, live-ops store, WebSocket service, worker terminals, Procurement/Inventory views.

</details>

<details>
<summary><b>CI enforcement</b></summary>

Every push to `develop`/`main` runs Checkstyle + JUnit (backend), ruff + Pytest with coverage (analytics), and ESLint + Vitest + build (frontend) — see [CI/CD](#cicd) below.

</details>

---

## CI/CD

![CI/CD](https://github.com/Iskandar-Mhadhbi/NexusWMS/actions/workflows/ci.yml/badge.svg?branch=develop)

```
Push to develop or main
        │
        ├── Backend: Checkstyle → JUnit → coverage artifact
        ├── Analytics: ruff → Pytest + coverage → coverage artifact
        └── Frontend: ESLint → Vitest + coverage → build → coverage artifact
                        │
                        ▼ (tests pass)
        ┌───────────────┴───────────────┐
        ▼                               ▼
  Build backend image             Build analytics image
        │                               │
        └───────────────┬───────────────┘
                         ▼ (main only)
              Trivy scan (CRITICAL + HIGH)
                         │
                         ▼
              Push to GHCR (tagged SHA + latest)
```

Images published to GitHub Container Registry:
- `ghcr.io/iskandar-mhadhbi/nexuswms/backend`
- `ghcr.io/iskandar-mhadhbi/nexuswms/analytics`

Frontend Docker image is intentionally not built yet — no `frontend/Dockerfile` exists; the job is scaffolded and commented out in `ci.yml` until one does.

---

## Design System

<details>
<summary><b>Color encodes domain, everywhere</b></summary>

| Color | Domain | Meaning |
|---|---|---|
| Purple | Live Ops | Cross-domain, the flow rail itself |
| Blue | Procurement | Inbound, not yet on the floor |
| Slate | Inventory | At-rest, unassembled stock |
| Amber | Fulfillment | Active work — picking, packing |
| Green | Dispatch | Completed, left the building |
| Teal | Reports | Back-office, historical/forecast |
| Pink | Finance | Back-office, financial gatekeeping |
| Coral | Employees | Back-office, people |

Danger/bottleneck state gets its own color (not reused from any domain) so a red pulse inside an amber zone still reads as "alert," not as blended amber.

</details>

<details>
<summary><b>Two UI surfaces, one system</b></summary>

- **Manager shell** — light, data-dense, left-rail navigation, mouse/keyboard oriented
- **Worker terminals** — dark by default, one task per screen, one large primary action, tablet-oriented

Split at the routing/login level by role — a PICKER lands straight on their terminal with zero pillar navigation; ADMIN/MANAGER/FINANCE/INVENTORY_CONTROLLER get the full manager shell.

</details>

<details>
<summary><b>Layout & typography rules</b></summary>

- **Flex only, never CSS grid** — `display: flex` as template class names, all color/border/padding/typography in scoped SCSS
- All identifiers (SKU codes, employee IDs, tracking numbers) render in monospace — mirrors how they're actually read off a physical label or scanner
- System font stack only, no webfont loading — keeps worker terminals fast on lower-end tablets

</details>

---

## Engineering Decisions

<details>
<summary><b>Why Spring Boot 4 native API versioning over SpringDoc-first design?</b></summary>

Spring Boot 4's `configureApiVersioning` (path-segment versioning, `/api/v1/...`) conflicted with SpringDoc's `/v3/api-docs` route being misinterpreted as a versioned path. SpringDoc 3.0.3 was reintroduced later once the conflict was understood and worked around; Swagger is explicitly disabled in `application-prod.yml` rather than left reachable in production.

</details>

<details>
<summary><b>Why PostgreSQL AND MongoDB?</b></summary>

PostgreSQL owns everything requiring ACID guarantees and complex joins. MongoDB is used only where document schema genuinely varies by type — a receiving scan and a dispatch scan don't share a shape, and forcing that into nullable Postgres columns would be worse than a flexible document. This isn't polyglot persistence for its own sake: `Shipment` deliberately has **no** MongoDB audit trail (documented in its Javadoc) because Redis pub/sub already covers its lifecycle event and a second trail would just be duplication.

</details>

<details>
<summary><b>Why bounded, capped-retry number generation?</b></summary>

Every human-readable identifier (`orderNumber`, `poNumber`, `trackingNumber`, `taskNumber`) is generated via a bounded loop capped at `MAX_GENERATION_ATTEMPTS = 10`, throwing `IllegalStateException` past that — converted from earlier unbounded recursion/loops. One real gap closed during this pass: `Shipment.generateCarrierTracking()` had **zero** uniqueness check before the fix.

</details>

<details>
<summary><b>Why batch-fetch actor summaries instead of a shared cross-package DTO?</b></summary>

Seven entities record a "who did this" UUID (`createdBy`, `assignedTo`, `dispatchedBy`, etc.). Rather than introducing a new shared `ActorSummary` type imported across every consuming package, the existing SKU-enrichment precedent was repeated exactly: `UserService.getUserSummaries(Set<UUID>)` batch-fetches once per list call, consuming services pull `employeeId`/`name` off the returned map into their own DTO's plain `String` fields. No new type crosses a package boundary beyond the one DTO itself — consistent with the project's "repeat the pattern, don't abstract prematurely" rule.

</details>

<details>
<summary><b>A real bug this caught: aisle/shelf codes scoped globally instead of per-parent</b></summary>

`Aisle.code` and `Shelf.code` were originally globally unique across the *entire* warehouse — meaning two different zones could never both have an aisle called `A1`, which real warehouse naming conventions do constantly. Fixed with composite unique constraints (`zone_id, code` / `aisle_id, code`) via a new Flyway migration, verified live by creating the same aisle code in two different zones successfully.

</details>

<details>
<summary><b>Why LocalStack instead of real AWS?</b></summary>

Banking restrictions in Tunisia prevent opening a real AWS account. LocalStack provides an API-compatible AWS surface locally — the architecture targets real AWS (S3, EventBridge) and migrating off LocalStack would mean swapping an endpoint, not rewriting integration code.

</details>

---

## Roadmap

**Immediate**
- [ ] Verify `POST /api/v1/scan` MongoDB writes (currently unconfirmed)
- [ ] Retrofit remaining pasted-UUID SKU line items (Orders/POs/Invoices) onto the real SKU picker

**Near-term**
- [ ] Expand JUnit/Vitest coverage to `OrderService`, `DispatchService`, `GoodsReceiptService`, and remaining frontend views
- [ ] Docker Compose finalization (`application-docker.yml`, verified full `docker-compose up`)
- [ ] Kubernetes manifests

**Longer-term / idea stage**
- [ ] Transport cart entity — models pickers pushing a cart through a full route rather than per-item trips
- [ ] Weigh-checkpoint sensor reading at pack completion, blocking dispatch on weight mismatch
- [ ] Reorder alert → draft PO conversion, closing the loop back into Procurement
- [ ] LocalStack S3 integration for supplier documents and shipping labels

---

## License

MIT © [Iskandar Mhadhbi](https://github.com/Iskandar-Mhadhbi)
