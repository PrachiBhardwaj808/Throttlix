# Module Breakdown — Throttlix

## 1. Overview
This document breaks down the **Throttlix** platform into modular, decoupled subsystems, establishing clear boundaries, contracts, inputs, outputs, and ownership.

---

## 2. Subsystem Module Specifications

### MOD-01: Authentication & API Key Management
- **Module ID:** `MOD-01`
- **Module Name:** Authentication & Key Management Subsystem
- **Purpose:** Manages client API key generation, cryptographic hashing, revocation, and runtime authentication of incoming requests.
- **Responsibilities:**
  - Generate cryptographically secure keys (`SecureRandom`).
  - Compute SHA-256 digests of API keys for database persistence.
  - Validate the `X-API-Key` header on ingress requests against active keys in MySQL.
  - Revoke and rotate existing keys upon administrative command.
- **Inputs:** Client ID, Key metadata, Raw API key header string.
- **Outputs:** Plaintext key (once at creation), Authenticated `ClientContext` object, or `AuthenticationException`.
- **Dependencies:** `com.throttlix.dao.ApiKeyDAO`, MySQL.
- **Related Database Entities:** `clients`, `api_keys`.
- **APIs Involved:** `POST /api/v1/clients/{id}/keys`, `PATCH /api/v1/keys/{id}/deactivate`.
- **Assigned Developer:** `[TO BE CONFIRMED]`
- **Acceptance Criteria:**
  - Raw secret displayed only once.
  - Invalid keys return HTTP 401; deactivated keys return HTTP 403.
  - Constant-time hash verification.

---

### MOD-02: Policy Configuration Subsystem
- **Module ID:** `MOD-02`
- **Module Name:** Policy Configuration & Lookup Subsystem
- **Purpose:** Stores, validates, and manages rate-limiting policy rules and their bindings to client accounts.
- **Responsibilities:**
  - Create and update rate-limiting policies in MySQL.
  - Link/unlink policies to specific clients via `client_policies`.
  - Cache policy definitions in memory to eliminate repeated SQL queries per request.
- **Inputs:** Policy Name, Algorithm Type, Request Limit, Window Seconds / Refill Rate, Client ID.
- **Outputs:** `RateLimitPolicy` domain model instance.
- **Dependencies:** `com.throttlix.dao.RateLimitPolicyDAO`, MySQL.
- **Related Database Entities:** `rate_limit_policies`, `client_policies`.
- **APIs Involved:** `POST /api/v1/policies`, `GET /api/v1/policies/{id}`.
- **Assigned Developer:** `[TO BE CONFIRMED]`
- **Acceptance Criteria:**
  - Rejects policies with negative or zero limits.
  - Dynamic policy updates propagate to active rate checks without server restart.

---

### MOD-03: Rate Limiting Engine Subsystem
- **Module ID:** `MOD-03`
- **Module Name:** Concurrency-Safe Rate Limiting Engine
- **Purpose:** Evaluates incoming requests against active client policies using configured rate-limiting algorithms.
- **Responsibilities:**
  - Provide common `RateLimitStrategy` interface (`evaluate(String clientKey, RateLimitPolicy policy)`).
  - Implement `TokenBucketStrategy`: atomic token deduction and continuous refill.
  - Implement `SlidingWindowStrategy`: timestamp history maintenance and rolling window purge.
  - Maintain thread-safe in-memory state structures (`ConcurrentHashMap`).
- **Inputs:** Client Identifier, `RateLimitPolicy` object, Request arrival timestamp.
- **Outputs:** `RateLimitDecision` (Decision: `ALLOW` / `REJECT`, Remaining count, Reset epoch, Retry delay).
- **Dependencies:** `java.util.concurrent`, System high-resolution clock (`System.currentTimeMillis()`).
- **Related Database Entities:** None (runtime state is held in-memory for MVP).
- **APIs Involved:** Evaluated internally across all managed gateway routes.
- **Assigned Developer:** `[TO BE CONFIRMED]`
- **Acceptance Criteria:**
  - Zero double-allocation under high parallel concurrency.
  - In-memory check completes in $< 5\text{ms}$.
  - Returns accurate `Retry-After` seconds when limit is reached.

---

### MOD-04: Ingress Request Validation & Gateway Proxy
- **Module ID:** `MOD-04`
- **Module Name:** Servlet Gateway & Request Pipeline Subsystem
- **Purpose:** Intercepts external HTTP requests, validates syntax, orchestrates auth and rate checks, forwards allowed requests, and formats output.
- **Responsibilities:**
  - Intercept HTTP requests via Jakarta Servlets and Filters.
  - Validate presence and format of required headers.
  - Coordinate calls between `AuthService`, `PolicyService`, and `RateLimitEngine`.
  - Forward allowed requests to target backend service and capture response.
  - Populate rate-limit headers (`X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`).
  - Dispatch telemetry logging asynchronously.
- **Inputs:** `HttpServletRequest`, Target backend URL configuration.
- **Outputs:** `HttpServletResponse` with client payload or JSON error structure.
- **Dependencies:** `MOD-01`, `MOD-02`, `MOD-03`, `MOD-05`, Jackson.
- **Related Database Entities:** None.
- **APIs Involved:** `GET /api/v1/demo/*`, `ANY /api/v1/gateway/*`.
- **Assigned Developer:** `[TO BE CONFIRMED]`
- **Acceptance Criteria:**
  - Returns HTTP 429 when engine rejects request.
  - Returns standard JSON error payload on 400, 401, 403, 429, 500.

---

### MOD-05: Audit & Request Logging Subsystem
- **Module ID:** `MOD-05`
- **Module Name:** Telemetry & Audit Logging Subsystem
- **Purpose:** Records complete metadata for every processed request to support compliance, troubleshooting, and analytics.
- **Responsibilities:**
  - Extract request metadata (client ID, route, HTTP method, status code, outcome, processing latency).
  - Write records to `request_logs` via JDBC batching or asynchronous worker queue.
  - Strictly filter out raw API keys, authorization tokens, and request/response bodies.
- **Inputs:** `RequestLogRecord` model.
- **Outputs:** Persisted database records in MySQL.
- **Dependencies:** `com.throttlix.dao.RequestLogDAO`, MySQL.
- **Related Database Entities:** `request_logs`.
- **APIs Involved:** None directly; invoked internally by `MOD-04`.
- **Assigned Developer:** `[TO BE CONFIRMED]`
- **Acceptance Criteria:**
  - Logging never blocks or degrades client response latency.
  - Zero secrets or sensitive payloads written to database.

---

### MOD-06: Analytics Aggregation API Subsystem
- **Module ID:** `MOD-06`
- **Module Name:** Analytics & Telemetry API Subsystem
- **Purpose:** Queries and aggregates raw request log data to supply data to the frontend dashboard.
- **Responsibilities:**
  - Execute optimized SQL aggregation queries (total requests, allowed vs. 429 counts, average latency).
  - Aggregate usage by client/key to identify top consumers.
  - Serve structured JSON payloads for frontend consumption.
- **Inputs:** Time range parameters, Client filter, Endpoint filter.
- **Outputs:** JSON analytics summary (`AnalyticsSummaryDTO`).
- **Dependencies:** `com.throttlix.dao.RequestLogDAO`, MySQL.
- **Related Database Entities:** `request_logs`, `clients`.
- **APIs Involved:** `GET /api/v1/analytics/summary`, `GET /api/v1/analytics/clients`.
- **Assigned Developer:** `[TO BE CONFIRMED]`
- **Acceptance Criteria:**
  - Sub-second query execution using MySQL indexes on `(client_id, requested_at)`.
  - Accurately mirrors database records.

---

### MOD-07: React Dashboard Subsystem
- **Module ID:** `MOD-07`
- **Module Name:** Web Administration & Analytics Dashboard
- **Purpose:** Provides a modern graphical user interface for operators to monitor traffic, manage keys, and view platform metrics.
- **Responsibilities:**
  - Render metric cards: Total Requests, Success Rate, 429 Rejections, Average Latency.
  - Render dynamic time-series charts using Recharts.
  - Display sortable client usage table.
  - Provide forms for client creation and policy assignment.
- **Inputs:** User clicks/inputs, REST responses from `MOD-06`.
- **Outputs:** Rendered DOM elements, User action dispatches.
- **Dependencies:** React 19, TypeScript, Tailwind CSS, Recharts, Axios / Fetch.
- **Related Database Entities:** Consumes data derived from all entities.
- **APIs Involved:** `GET /api/v1/analytics/*`, `GET/POST /api/v1/clients/*`.
- **Assigned Developer:** `[TO BE CONFIRMED]`
- **Acceptance Criteria:**
  - Fully responsive across desktop and tablet viewports.
  - Handles backend disconnection gracefully with user-friendly alerts.
