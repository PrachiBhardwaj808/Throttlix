# Test Cases & Verification Scenarios — Throttlix

## 1. Test Suite Overview
This document catalogs the core test cases for validating the **Throttlix** platform, covering algorithm correctness, concurrency safety, authentication security, and API response fidelity.

---

## 2. Test Case Specifications

### TC-01: Token Bucket Burst Tolerance and Refill
- **Test ID:** `TC-01`
- **Component:** `TokenBucketStrategy`
- **Objective:** Verify that a client can burst up to capacity, encounters HTTP 429 when empty, and recovers tokens after the refill interval.
- **Preconditions:** Client policy has `capacity = 2`, `refillRate = 1` token/second. Bucket starts full (2 tokens).
- **Execution Steps:**
  1. Send Request 1 immediately.
  2. Send Request 2 immediately.
  3. Send Request 3 immediately.
  4. Wait for 1.1 seconds.
  5. Send Request 4.
- **Expected Results:**
  - Request 1: Decision `ALLOW`, remaining $= 1$, status `200`.
  - Request 2: Decision `ALLOW`, remaining $= 0$, status `200`.
  - Request 3: Decision `REJECT`, status `429 Too Many Requests`, header `Retry-After: 1`.
  - Request 4: Decision `ALLOW`, remaining $= 0$, status `200`.

---

### TC-02: Sliding Window Boundary Precision
- **Test ID:** `TC-02`
- **Component:** `SlidingWindowStrategy`
- **Objective:** Verify that request timestamps outside the rolling window expire cleanly.
- **Preconditions:** Client policy has `limit = 2`, `windowSeconds = 10`.
- **Execution Steps:**
  1. Send Request 1 at $t = 0\text{s}$.
  2. Send Request 2 at $t = 2\text{s}$.
  3. Send Request 3 at $t = 4\text{s}$.
  4. Wait until $t = 10.5\text{s}$ (Request 1 moves out of window).
  5. Send Request 4 at $t = 11\text{s}$.
- **Expected Results:**
  - Requests 1 and 2: Allowed.
  - Request 3: Rejected with HTTP `429`.
  - Request 4: Allowed (window currently contains Request 2 and Request 4, total count $= 2$).

---

### TC-03: Concurrency Safety Under Parallel Contention
- **Test ID:** `TC-03`
- **Component:** `RateLimitEngine` & Concurrency Locks
- **Objective:** Guarantee that simultaneous threads cannot double-spend a remaining token.
- **Preconditions:** Token bucket initialized with `capacity = 5`, `refillRate = 0`.
- **Execution Steps:**
  1. Create 50 parallel threads in an `ExecutorService`.
  2. Synchronize thread start using a `CountDownLatch`.
  3. All 50 threads invoke `evaluate()` concurrently.
- **Expected Results:**
  - Exactly 5 requests receive `ALLOW`.
  - Exactly 45 requests receive `REJECT`.
  - Allowed count never exceeds 5 under any execution order.

---

### TC-04: Missing API Key Header
- **Test ID:** `TC-04`
- **Component:** `AuthService` & Gateway Servlet
- **Objective:** Verify that unauthenticated requests are stopped at the perimeter.
- **Execution Steps:**
  1. Issue `GET /api/v1/demo/orders` without the `X-API-Key` header.
- **Expected Results:**
  - HTTP `401 Unauthorized`.
  - JSON payload: `{"error": "UNAUTHORIZED", "message": "Missing API key"}`.
  - Target backend service is **never** invoked.

---

### TC-05: Deactivated or Revoked API Key
- **Test ID:** `TC-05`
- **Component:** `AuthService` & `ApiKeyDAO`
- **Objective:** Verify that deactivated credentials cannot access services.
- **Preconditions:** Key exists in database with `active = false`.
- **Execution Steps:**
  1. Issue `GET /api/v1/demo/orders` with `X-API-Key: <deactivated_key>`.
- **Expected Results:**
  - HTTP `403 Forbidden`.
  - JSON payload: `{"error": "FORBIDDEN", "message": "API key has been deactivated"}`.

---

### TC-06: Database Constraint Enforcement via JDBC
- **Test ID:** `TC-06`
- **Component:** `ClientDAO`
- **Objective:** Ensure duplicate client names trigger controlled errors.
- **Execution Steps:**
  1. Create client with name `"Alpha"`.
  2. Attempt to create another client with name `"Alpha"`.
- **Expected Results:**
  - System catches PostgreSQL unique constraint violation (`23505`).
  - Returns HTTP `409 Conflict` without leaking SQL statement or database stack trace.

---

### TC-07: Telemetry Logging Accuracy
- **Test ID:** `TC-07`
- **Component:** `RequestLogDAO`
- **Objective:** Ensure all processed requests produce complete audit rows.
- **Execution Steps:**
  1. Call `/api/v1/demo/orders` with valid key (allowed).
  2. Call `/api/v1/demo/orders` exceeding limit (rejected).
  3. Query `request_logs` in PostgreSQL.
- **Expected Results:**
  - Row 1: `status_code = 200`, `allowed = true`, `endpoint = '/api/v1/demo/orders'`, valid `latency_ms`.
  - Row 2: `status_code = 429`, `allowed = false`, `endpoint = '/api/v1/demo/orders'`.
  - Neither row contains raw API key secrets.
