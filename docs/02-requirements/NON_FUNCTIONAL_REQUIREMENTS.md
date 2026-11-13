# Non-Functional Requirements — Throttlix

## 1. Overview
Non-Functional Requirements (NFRs) specify the operational qualities, technical constraints, performance benchmarks, and security controls governing the **Throttlix** platform.

---

## 2. Detailed NFR Specifications

### NFR-01: Performance & Latency
- **Benchmark:** The internal gateway processing overhead—including header parsing, API key hash lookup, and in-memory rate evaluation—must not exceed **5 milliseconds** ($p95$) for allowed requests under normal system load.
- **Verification:** Benchmarked via automated JMeter / k6 load tests running against the `/api/v1/demo/orders` endpoint on local test environments.

### NFR-02: Concurrency & Thread Safety
- **Requirement:** The rate-limiting algorithms must be strictly thread-safe under high-concurrency conditions.
- **Controls:**
  - Token refill calculations and counter decrements must execute atomically.
  - Shared in-memory data structures must utilize `ConcurrentHashMap` or explicit synchronization blocks.
  - Concurrency tests must verify that under $M$ simultaneous threads attempting to consume the final remaining token, exactly one thread succeeds and $M - 1$ threads are rejected with HTTP 429.

### NFR-03: System Reliability & Fault Tolerance
- **Requirement:** The gateway must maintain continuous operation and graceful error handling during component failures.
- **Controls:**
  - Malformed request headers, illegal characters, or database timeouts must result in controlled, standardized HTTP error responses (`400`, `500`), never an unhandled servlet container crash or JVM termination.
  - If the database becomes temporarily unreachable, active in-memory rate limits must continue protecting downstream endpoints based on cached policy state where feasible.

### NFR-04: Security & Cryptographic Controls
- **Requirement:** Confidentiality of client credentials and defense against injection vulnerabilities.
- **Controls:**
  - **Zero Plaintext Secrets:** Raw API keys must never be stored in PostgreSQL or logged. Keys must be cryptographically hashed (e.g., SHA-256) upon generation.
  - **SQL Injection Prevention:** All database operations must strictly utilize parameterized queries via JDBC `PreparedStatement`. String concatenation for SQL queries is strictly prohibited.
  - **Transport Security:** All external client-to-gateway and dashboard-to-gateway traffic in staging/production must enforce HTTPS (TLS 1.3).
  - **Information Hiding:** HTTP error responses must never expose database schemas, internal class names, or stack traces to callers.

### NFR-05: Maintainability & Clean Architecture
- **Requirement:** High codebase readability, modularity, and low coupling to support long-term extensibility.
- **Controls:**
  - Strict adherence to the 4-layer architecture: `Controller (Servlet)` $\rightarrow$ `Service` $\rightarrow$ `DAO / Repository` $\rightarrow$ `Model`.
  - Application of SOLID object-oriented principles, using strategy interfaces (`RateLimitStrategy`) to isolate algorithm implementations.
  - Zero reliance on heavyweight reflection or magic annotation frameworks (Spring Boot, Hibernate).

### NFR-06: Scalability & Evolutionary Architecture
- **Requirement:** Clean abstraction boundaries enabling seamless future migration to distributed deployments.
- **Controls:**
  - The rate-limiting engine must interact with state through clean interfaces, allowing in-memory `ConcurrentHashMap` state to be replaced with a Redis-backed distributed counter implementation in Phase 2 without modifying servlet routing logic.

### NFR-07: Data Integrity & Consistency
- **Requirement:** ACID transactional guarantees for relational data.
- **Controls:**
  - Foreign key constraints, unique indexes, and non-nullable column rules enforced at the PostgreSQL database level.
  - Database transactions (`setAutoCommit(false)`, `commit()`, `rollback()`) applied whenever multiple related tables are mutated simultaneously.

### NFR-08: Usability & Observability
- **Requirement:** Transparent operational diagnostics for administrators and intuitive feedback for API consumers.
- **Controls:**
  - API consumers receive standard RFC-compliant headers (`X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`).
  - React dashboard provides readable charts and tabular views with real-time response latency metrics.
