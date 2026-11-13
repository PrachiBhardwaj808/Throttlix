# Functional Requirements — Throttlix

## 1. Requirement Classification Framework
Requirements are categorized by their implementation priority:
- **Core MVP (FR-01 to FR-12):** Essential requirements for the initial working platform.
- **Future Enhancements (FR-13 to FR-16):** Documented architectural enhancements for post-MVP phases.

---

## 2. Core MVP Functional Requirements

### FR-01: API Key Lifecycle Management
- **Description:** The system must provide administrative capabilities to create, list, inspect metadata for, and deactivate client API keys. The raw secret is displayed only once at creation time; subsequent persistence and comparisons use secure cryptographic hashes.
- **Inputs:** Client ID, Key Name, Expiry Date (optional).
- **Outputs:** Plaintext Key (shown once to admin), Key Metadata (ID, masked prefix, created timestamp, active state).
- **Acceptance Criteria:**
  - A newly generated key is immediately usable for authentication.
  - A deactivated or expired key is rejected during authentication.
  - Stored database records contain only one-way cryptographic hashes (e.g., SHA-256).

### FR-02: API Key Authentication
- **Description:** The system must extract the `X-API-Key` header from incoming HTTP requests, hash the provided secret, and verify against active database records before permitting request progression.
- **Inputs:** Ingress HTTP Header `X-API-Key`.
- **Outputs:** Authenticated Client Context or HTTP `401 Unauthorized` / `403 Forbidden` response.
- **Acceptance Criteria:**
  - Requests missing `X-API-Key` immediately return `401 Unauthorized` with a standard JSON error message.
  - Requests with unknown or malformed keys return `401 Unauthorized`.
  - Requests with valid but deactivated keys return `403 Forbidden`.
  - Unauthenticated requests never reach the rate-limiting evaluation engine.

### FR-03: Token Bucket Rate-Limiting Algorithm
- **Description:** The rate-limiting engine must implement the Token Bucket algorithm. Each client possesses a virtual bucket holding up to a maximum token capacity. Tokens refill continuously at a configured refill rate. Each processed request atomically consumes one token.
- **Inputs:** Policy definition (Capacity $C$, Refill Rate $R$), Current bucket state (Stored tokens, last refill timestamp).
- **Outputs:** Allow / Reject decision, Remaining token count, Reset duration.
- **Acceptance Criteria:**
  - Accommodates short bursts up to capacity $C$.
  - Refill formula: $\text{currentTokens} = \min(C, \text{storedTokens} + \Delta t \times R)$.
  - Atomic token consumption guarantees zero race conditions under concurrent execution.

### FR-04: Sliding Window Rate-Limiting Algorithm
- **Description:** The system must implement a Sliding Window algorithm tracking request timestamps within a moving time window (e.g., $L$ requests per $W$ seconds). Old timestamps outside $(t_{\text{current}} - W)$ are discarded.
- **Inputs:** Policy definition (Limit $L$, Window Length $W$ in seconds), Client timestamp history.
- **Outputs:** Allow / Reject decision, Remaining request allowance within window.
- **Acceptance Criteria:**
  - Accurately limits requests across any arbitrary rolling window, eliminating fixed-window boundary burst exploits.
  - Discards timestamps older than the active window length.

### FR-05: Configurable Rate-Limit Policies
- **Description:** Administrators must be able to define custom rate-limiting policies and dynamically link them to specific clients.
- **Inputs:** Policy Name, Algorithm Type (`TOKEN_BUCKET`, `SLIDING_WINDOW`), Request Limit / Capacity, Time Window (seconds) / Refill Rate, Enabled Status.
- **Outputs:** Persisted policy record linked to target clients.
- **Acceptance Criteria:**
  - Different clients can be assigned different policies simultaneously (e.g., Free Tier: 10 req/min; Enterprise: 1000 req/min).
  - Policy updates take effect immediately on subsequent requests.

### FR-06: Ingress Request Validation
- **Description:** The gateway servlet layer must inspect and validate all incoming HTTP requests prior to backend forwarding or rate evaluation.
- **Inputs:** HTTP Method, Path URI, Headers, Query Parameters.
- **Outputs:** Validated request or HTTP `400 Bad Request`.
- **Acceptance Criteria:**
  - Rejects malformed URIs, illegal characters, or missing mandatory headers with JSON-structured error responses.

### FR-07: Standardized HTTP Response Handling
- **Description:** The gateway must return standardized HTTP status codes and structured JSON response bodies for all gateway-handled outcomes.
- **Inputs:** Processing outcome (Success, Authentication Failure, Rate Limit Rejection, System Error).
- **Outputs:** Deterministic HTTP status codes (`200`, `201`, `400`, `401`, `403`, `429`, `500`).
- **Acceptance Criteria:**
  - Error responses follow a uniform JSON schema (`{"error": "...", "message": "...", "timestamp": "..."}`).
  - Internal exceptions or database stack traces are never exposed in response bodies.

### FR-08: Rate-Limit Response Headers
- **Description:** Both allowed and rejected responses must include standard rate-limiting metadata headers.
- **Headers Exposed:**
  - `X-RateLimit-Limit`: Maximum requests permitted in the configured period.
  - `X-RateLimit-Remaining`: Requests remaining in the current window or bucket.
  - `X-RateLimit-Reset`: Epoch timestamp when the quota resets or refills.
  - `Retry-After`: Seconds to wait before retrying (mandatory on `429 Too Many Requests`).
- **Acceptance Criteria:**
  - Values must be mathematically accurate based on the active algorithm; values must not be estimated or guessed.

### FR-09: Request Audit & Telemetry Logging
- **Description:** Every processed request (allowed or rejected) must be recorded into the PostgreSQL database for historical auditing and analytics.
- **Logged Fields:** `log_id`, `client_id`, `api_key_id`, `endpoint`, `http_method`, `status_code`, `allowed` (boolean), `latency_ms`, `requested_at`.
- **Acceptance Criteria:**
  - Raw API key secrets and sensitive request bodies are strictly excluded from logs.
  - Logging overhead must not block or significantly degrade forward-request response latency.

### FR-10: Analytics Dashboard
- **Description:** A web-based user interface providing system administrators with high-level summaries of platform activity.
- **Outputs:** Total request count, Allowed vs. Rejected distribution chart, System error rate, Active key counts.
- **Acceptance Criteria:**
  - Visual charts render correctly via Recharts.
  - Dashboard consumes aggregated REST APIs (`GET /api/v1/analytics/summary`).

### FR-11: Client-Wise Usage Statistics
- **Description:** The dashboard must allow operators to filter and inspect consumption statistics broken down by individual client or API key.
- **Outputs:** Ranked list of busiest clients, rejection rate per client, top consumed endpoints per client.
- **Acceptance Criteria:**
  - Enables administrators to pinpoint abusive clients or clients requiring tier upgrades.

### FR-12: Real-Time Traffic Pattern Monitoring
- **Description:** Visual representation of recent traffic velocity and sudden volume spikes.
- **Outputs:** Time-series request frequency graphs and anomaly indicator flags for review.
- **Acceptance Criteria:**
  - Visualizes recent request velocity derived from recorded log data without initiating automatic IP bans.

---

## 3. Future Architectural Enhancements (Post-MVP)

| ID | Enhancement Name | Scope Description |
| :--- | :--- | :--- |
| **FR-13** | **Redis Shared Counter Integration** | Transition rate-limiting state from single-JVM memory to an external Redis cluster using atomic increment commands (`INCR`, `EXPIRE`) for distributed deployments. |
| **FR-14** | **Bounded Request Queueing** | Implement a temporary in-memory queue to delay execution of eligible burst requests rather than rejecting immediately with 429. Must be strictly bounded to prevent memory exhaustion. |
| **FR-15** | **Automated Anomaly Detection** | Statistical and machine learning analysis of request frequency and payload size to detect distributed bot attacks and flag suspicious keys for operator review. |
| **FR-16** | **IP-Based Throttling & Restrictions** | Extend policy evaluation to client IP addresses (`X-Forwarded-For`), enabling CIDR block quotas and global defense against unauthenticated spam. |

