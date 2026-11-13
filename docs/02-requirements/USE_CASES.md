# Use Cases — Throttlix

## 1. Overview
This document specifies the primary operational use cases for the **Throttlix** platform, defining interactions between actors (Administrators, Consumers) and the system.

---

## 2. Standardized Use Cases

### UC-01: Authenticate Request and Allow In-Policy Traffic
- **Use Case ID:** `UC-01`
- **Use Case Name:** Authenticate Request and Forward Allowed Traffic
- **Primary Actor:** API Consumer / Client Application
- **Preconditions:**
  - Client has an active, valid API key.
  - Client has an assigned rate limit policy with available quota or tokens.
  - Protected backend endpoint is running and accessible.
- **Trigger:** Consumer issues an HTTP request to a Throttlix-managed endpoint with the `X-API-Key` header.
- **Main Success Scenario:**
  1. Throttlix servlet intercepts the incoming HTTP request.
  2. Gateway extracts the `X-API-Key` header and validates format.
  3. Authentication service hashes the key and queries PostgreSQL for an active client record.
  4. System retrieves the active policy (e.g., Token Bucket with capacity 10, refill 1/s).
  5. Rate-limiting engine checks current state; confirms tokens $\ge 1$.
  6. Engine atomically decrements 1 token from the bucket.
  7. Gateway forwards the request to the configured protected backend service.
  8. Gateway receives the downstream backend response.
  9. Gateway appends rate-limit headers (`X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`).
  10. System persists an audit log record into `request_logs` with status `200` and `allowed = true`.
  11. HTTP 200 response with backend body and headers is returned to the Consumer.
- **Alternative Flows:** None.
- **Exception Flows:**
  - *EF-1 (Backend Service Down):* Downstream service returns 502/503. Gateway logs the failure, appends rate headers, and returns `502 Bad Gateway` to Consumer.
- **Postconditions:** One token is consumed; request telemetry is durably recorded in PostgreSQL.

---

### UC-02: Reject Excess Traffic Exceeding Rate Limit Policy
- **Use Case ID:** `UC-02`
- **Use Case Name:** Rate-Limit Rejection on Quota Exceeded
- **Primary Actor:** API Consumer / Client Application
- **Preconditions:**
  - Client possesses an active API key.
  - Client has consumed all available tokens or reached the maximum window capacity.
- **Trigger:** Consumer issues an HTTP request while their quota is exhausted.
- **Main Success Scenario:**
  1. Throttlix servlet intercepts the incoming request.
  2. Key is authenticated and client policy is retrieved.
  3. Rate-limiting engine evaluates policy; determines available tokens $= 0$ or window count $\ge \text{limit}$.
  4. Rate-limiting engine issues a `REJECT` decision and calculates reset/retry delay.
  5. Gateway halts request processing immediately; request is **not** forwarded to the protected backend.
  6. Gateway constructs an HTTP `429 Too Many Requests` response.
  7. Gateway sets headers: `X-RateLimit-Remaining: 0`, `Retry-After: <seconds>`, `X-RateLimit-Reset: <timestamp>`.
  8. System records an audit log record in `request_logs` with status `429` and `allowed = false`.
  9. Structured JSON error body (`{"error": "RATE_LIMIT_EXCEEDED", "message": "Try again after X seconds"}`) is returned to the Consumer.
- **Alternative Flows:** None.
- **Exception Flows:** None.
- **Postconditions:** Backend server remains completely protected from compute load; rejection is logged.

---

### UC-03: Reject Request on Missing or Invalid API Key
- **Use Case ID:** `UC-03`
- **Use Case Name:** Authentication Rejection on Invalid Credentials
- **Primary Actor:** API Consumer / Client Application
- **Preconditions:** None.
- **Trigger:** Consumer sends a request without `X-API-Key` or with an unrecognized secret.
- **Main Success Scenario:**
  1. Gateway intercepts request and inspects headers.
  2. If `X-API-Key` header is missing, or hashed secret is not found in `api_keys`:
  3. Gateway halts execution immediately; rate limiter and backend are bypassed.
  4. System logs the unauthenticated attempt.
  5. HTTP `401 Unauthorized` with JSON error payload (`{"error": "UNAUTHORIZED", "message": "Missing or invalid API key"}`) is returned to Consumer.
- **Alternative Flows:**
  - *AF-1 (Key Deactivated / Revoked):* Key exists in DB but `active = false`. Gateway returns HTTP `403 Forbidden` (`{"error": "FORBIDDEN", "message": "API key has been deactivated"}`).
- **Exception Flows:** None.
- **Postconditions:** No downstream resources are consumed; unauthenticated attempt is audited.

---

### UC-04: Onboard Client and Provision API Key
- **Use Case ID:** `UC-04`
- **Use Case Name:** Onboard Client Organization and Generate Credential
- **Primary Actor:** API Administrator
- **Preconditions:** Administrator is authorized to manage clients.
- **Trigger:** Administrator submits client creation or key generation request via UI or REST API.
- **Main Success Scenario:**
  1. Admin submits client details (`POST /api/v1/clients` with name and description).
  2. System creates new client record in `clients` table.
  3. Admin requests API key creation (`POST /api/v1/clients/{id}/keys`).
  4. System generates a cryptographically secure random API key string (e.g., `tk_live_...`).
  5. System computes the SHA-256 hash of the generated secret.
  6. System persists the hash and metadata into `api_keys` table.
  7. System returns HTTP 201 with the plaintext key secret in the response body.
  8. Admin UI displays the plaintext secret with an explicit warning that it will never be displayed again.
- **Alternative Flows:** None.
- **Exception Flows:**
  - *EF-1 (Duplicate Client Name):* Database unique constraint triggers. System returns HTTP `409 Conflict`.
- **Postconditions:** Client record and hashed key are persisted; client can begin authenticating.

---

### UC-05: Configure and Assign Rate-Limiting Policy
- **Use Case ID:** `UC-05`
- **Use Case Name:** Configure and Dynamically Assign Rate Limit Policy
- **Primary Actor:** API Administrator
- **Preconditions:** Target client organization exists.
- **Trigger:** Administrator creates or updates a policy definition.
- **Main Success Scenario:**
  1. Admin submits policy parameters (`POST /api/v1/policies`): algorithm (`TOKEN_BUCKET`), limit (100), window/refill (60s).
  2. System validates parameters (positive limit, supported algorithm).
  3. System persists policy into `rate_limit_policies`.
  4. Admin associates policy with client in `client_policies`.
  5. Gateway immediately loads updated policy upon subsequent requests from that client.
- **Alternative Flows:** None.
- **Exception Flows:**
  - *EF-1 (Invalid Algorithm Specified):* System rejects request with HTTP `400 Bad Request`.
- **Postconditions:** Client's future traffic is evaluated against the new policy parameters.

---

### UC-06: View Operational Analytics & Traffic Telemetry
- **Use Case ID:** `UC-06`
- **Use Case Name:** Inspect Platform Traffic & Performance Telemetry
- **Primary Actor:** API Administrator / System Auditor
- **Preconditions:** System has processed incoming requests and populated `request_logs`.
- **Trigger:** Administrator opens the React Analytics Dashboard.
- **Main Success Scenario:**
  1. Frontend dashboard requests aggregated metrics (`GET /api/v1/analytics/summary`).
  2. Analytics servlet executes optimized aggregation queries against PostgreSQL `request_logs`.
  3. System calculates total requests, allowed count, rejected count (429s), and average latency.
  4. Backend returns JSON summary payload.
  5. React UI renders metric cards, Recharts time-series volume graph, and client usage ranking table.
- **Alternative Flows:** None.
- **Exception Flows:**
  - *EF-1 (Database Timeout):* Dashboard displays a graceful error banner and cached/empty state.
- **Postconditions:** Administrator gains operational visibility into traffic distribution and rejection trends.
