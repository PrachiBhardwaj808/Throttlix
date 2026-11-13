# Project Scope — Throttlix

## 1. Document Purpose
This document establishes the official boundaries for the **Throttlix** project. It explicitly defines what is included in the initial Minimum Viable Product (MVP), what is designated as secondary or future scope, and what is excluded from the system architecture.

---

## 2. Scope Classification Matrix

| Scope Tier | Classification Description | Deliverables Included |
| :--- | :--- | :--- |
| **Tier 1: MVP Core (Must Have)** | Critical baseline deliverables required for system operation, demonstration, and evaluation. | - API Key Generation, Hashing, & Revocation (FR-01)<br>- API Key Authentication via `X-API-Key` (FR-02)<br>- In-Memory Thread-Safe Token Bucket Algorithm (FR-03)<br>- In-Memory Sliding Window Algorithm (FR-04)<br>- Dynamic Rate Limit Policies per Client (FR-05)<br>- Ingress Request Validation & Parameter Sanitization (FR-06)<br>- Standardized HTTP Response Handling (FR-07)<br>- Rate Limit Telemetry Response Headers (FR-08)<br>- PostgreSQL Request & Audit Logging via raw JDBC (FR-09)<br>- React + TypeScript Analytics Dashboard (FR-10) |
| **Tier 2: MVP Secondary** | High-value additions delivered within the core timeline if capacity permits. | - Client-wise Usage Statistics Breakdown (FR-11)<br>- Recent Traffic Activity & Frequency Visualization (FR-12)<br>- HikariCP Connection Pool Optimization |
| **Tier 3: Future Enhancements (Post-MVP)** | Architectural evolution and distributed features; not required for initial delivery. | - Redis Integration for Distributed Multi-Node Shared Counters (FR-13)<br>- Bounded Request Queueing for Asynchronous Work (FR-14)<br>- Traffic Anomaly & Abuse Pattern Detection (FR-15)<br>- IP-Address-Based Quotas & Restrictions (FR-16)<br>- Docker Compose Multi-Service Local Containerization |
| **Tier 4: Out of Scope** | Features explicitly excluded from current and near-term architectural plans. | - Payment Gateway & Automated Billing Processing<br>- OAuth2 / OIDC Third-Party Social Logins<br>- Distributed Tracing (e.g., OpenTelemetry, Jaeger)<br>- Multi-Region Active-Active Database Replication<br>- Autonomous AI-Based Dynamic IP Blacklisting |

---

## 3. Detailed Scope Boundaries

### 3.1 Backend Architecture Boundary
- **Included:** Java 21 LTS, Jakarta Servlet API 6.0, Apache Tomcat 10.1+, JDBC persistence layer, Jackson JSON processing, JUnit 5, and Mockito.
- **Excluded:** Spring Framework (Spring Boot, Spring MVC, Spring Data), Hibernate, JPA, or enterprise container services. The backend must remain a pure Java application deployed as a standard WAR archive.

### 3.2 State Management & Concurrency Boundary
- **Single-Node In-Memory MVP:** For the initial version, rate-limiting counters (Token Bucket state and Sliding Window timestamp queues) reside safely in single-server JVM memory using thread-safe data structures (`ConcurrentHashMap`, `AtomicLong`).
- **Distributed State Clarification:** Shared counters across multi-node server clusters via Redis are strictly designated as a future enhancement once single-instance correctness is verified.

### 3.3 Security & Secret Handling Boundary
- Raw API key secrets are displayed exactly once at creation time.
- The database stores only secure cryptographic hashes of keys.
- Application logs and dashboard APIs must never record or expose raw API key secrets, request bodies, or client credentials.
