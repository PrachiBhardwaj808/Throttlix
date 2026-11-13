# Architecture Decision Log (ADR) — Throttlix

## 1. Overview
This log documents the key architectural and technical decisions made during the design and development of **Throttlix**, detailing the context, options evaluated, rationale, and consequences.

---

## 2. Decision Records

### ADR-001: Selection of Core Java & Jakarta Servlets over Spring Boot
- **Decision ID:** `ADR-001`
- **Date:** 2026-11-06
- **Status:** **Accepted**
- **Context:**
  The team is building a high-performance, low-latency API traffic-control gateway. We needed to choose the backend web framework and runtime architecture.
- **Options Considered:**
  1. *Spring Boot / Spring Cloud Gateway:* Heavy ecosystem with automatic dependency injection and embedded Netty/Tomcat.
  2. *Core Java 21 + Jakarta Servlets 6.0 + Apache Tomcat 10.1:* Standard Java web technology with direct request/response control.
- **Decision:**
  Use **Core Java 21** and **Jakarta Servlets 6.0** deployed as a WAR archive to **Apache Tomcat 10.1+**. Do **not** use Spring Boot.
- **Reasoning:**
  - Matches the team's strong foundation in Java and Tomcat.
  - Eliminates framework overhead, reflection delays, and dependency bloat.
  - Gives complete transparency over servlet filters, request streams, and response headers.
- **Consequences:**
  - Standard, lightweight WAR packaging.
  - Requires manual wiring of services and connection pools instead of automatic Spring annotations.

---

### ADR-002: Native JDBC over Object-Relational Mapping (Hibernate / JPA)
- **Decision ID:** `ADR-002`
- **Date:** 2026-11-06
- **Status:** **Accepted**
- **Context:**
  The gateway must query API keys and insert telemetry log rows for high-volume traffic with minimal latency overhead.
- **Options Considered:**
  1. *Hibernate / Spring Data JPA:* Automatic ORM mapping, entity caching, and query generation.
  2. *Native JDBC (`PreparedStatement`, `ResultSet`):* Direct SQL execution over JDBC.
- **Decision:**
  Use **native JDBC** with explicit, parameterized SQL queries.
- **Reasoning:**
  - Provides deterministic, low-overhead SQL execution without ORM session overhead or dirty checking.
  - Guarantees full control over indexes, transactions, and batch inserts.
  - Matches the team's SQL and JDBC expertise.
- **Consequences:**
  - Writing explicit SQL queries in DAO classes.
  - Explicit connection handling and parameter binding (`PreparedStatement`).

---

### ADR-003: Single-Instance In-Memory Limiter for MVP, Redis for Post-MVP
- **Decision ID:** `ADR-003`
- **Date:** 2026-11-06
- **Status:** **Accepted**
- **Context:**
  Rate-limiting counters must update and evaluate rapidly on every incoming request. We needed to choose where counter state resides.
- **Options Considered:**
  1. *Relational PostgreSQL writes for every counter:* Querying/updating SQL on every request.
  2. *Immediate Redis Distributed Cluster:* External in-memory store for shared counters.
  3. *In-Memory Java State for MVP, Redis as Future Enhancement:* Fast single-JVM state first; Redis later.
- **Decision:**
  Deliver an **in-memory, single-instance rate limiter** using thread-safe Java structures (`ConcurrentHashMap`, `AtomicLong`) for the MVP. Design the strategy interface so Redis can be introduced post-MVP without changing servlet logic.
- **Reasoning:**
  - SQL writes on every request would saturate database connections and introduce latency.
  - Requiring a distributed Redis cluster in Week 1 threatens project deadlines and complicates local setup.
  - An in-memory implementation proves algorithm correctness first with sub-millisecond execution.
- **Consequences:**
  - In-memory counter state resets if Tomcat restarts.
  - Enables clean, risk-free MVP delivery followed by Redis clustering in Phase 2.

---

### ADR-004: Cryptographic Hashing (SHA-256) for Stored API Keys
- **Decision ID:** `ADR-004`
- **Date:** 2026-11-06
- **Status:** **Accepted**
- **Context:**
  API keys authenticate callers. If raw keys are compromised through database leaks or logs, protected services become vulnerable.
- **Options Considered:**
  1. *Plaintext Storage:* Storing raw keys directly in `api_keys`.
  2. *Two-Way Encryption (AES-GCM):* Encrypting keys with a master server key.
  3. *One-Way Cryptographic Hashing (SHA-256):* Storing only SHA-256 hex digests.
- **Decision:**
  Store only **one-way SHA-256 cryptographic hashes** in PostgreSQL. Return the plaintext key **only once** upon generation.
- **Reasoning:**
  - Standard industry practice (similar to GitHub, Stripe, AWS key management).
  - Even in the event of a database compromise, raw API keys cannot be reverse-engineered.
- **Consequences:**
  - Lost keys cannot be retrieved; they must be revoked and regenerated.
