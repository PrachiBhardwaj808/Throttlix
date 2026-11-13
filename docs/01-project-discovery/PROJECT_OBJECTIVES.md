# Project Objectives — Throttlix

## 1. Executive Intent
The primary objective of **Throttlix** is to deliver an enterprise-grade, lightweight API traffic management platform that guarantees service availability, isolates client quotas, and provides actionable observability without relying on heavy application frameworks.

---

## 2. Business Objectives
- **BO-01: Protect Core Infrastructure:** Ensure downstream protected APIs maintain 99.9% uptime during external traffic surges.
- **BO-02: Fair Allocation of Computing Resources:** Eliminate client starvation by enforcing strict rate limit policies per registered API key.
- **BO-03: Accelerate Client Integration:** Provide transparent client communication via standardized HTTP status codes and rate-limit headers (`X-RateLimit-*`).
- **BO-04: Centralized Governance:** Enable administrators to manage clients, keys, and rate policies dynamically through an intuitive web interface.

---

## 3. Technical & Architectural Objectives
- **TO-01: Low Overhead & High Performance:** Ensure that the rate-limiting evaluation and request validation pipeline introduces minimal latency (< 5ms in-memory check) to forwarded requests.
- **TO-02: Framework Independence:** Implement the core backend using standard **Java 21 LTS**, **Jakarta Servlets 6.0**, and **raw JDBC** over **Apache Tomcat 10.1+**, eliminating Spring Boot and ORM overhead.
- **TO-03: Thread-Safe State Management:** Guarantee strict concurrency safety for rate-limiting counters under high-concurrency workloads using atomic primitives and thread-safe data structures (`ConcurrentHashMap`, atomic time-tracking).
- **TO-04: Multi-Algorithm Support:** Provide extensible algorithm implementations, delivering both **Token Bucket** (for burst tolerance) and **Sliding Window** (for strict temporal enforcement).
- **TO-05: Secure Credential Management:** Enforce one-way cryptographic hashing for stored API keys, ensuring raw secrets are never exposed in logs, database tables, or monitoring dashboards.

---

## 4. Quality & Operational Goals

| Goal Area | Target Metric / Standard | Verification Method |
| :--- | :--- | :--- |
| **Unit Test Coverage** | Minimum 85% branch coverage on algorithm strategies and validation services. | Automated Maven Surefire / JUnit 5 test runs. |
| **Concurrency Safety** | Zero race conditions or double-token spending under parallel simulated load. | Multi-threaded JUnit concurrency stress tests & JMeter/k6 benchmarks. |
| **Error Handling** | 100% controlled error responses; zero unhandled 500 exceptions leaking stack traces to clients. | Negative test suite with malformed payloads, invalid headers, and simulated DB faults. |
| **Code Maintainability** | Strict separation of concerns (Servlet -> Service -> DAO -> Model) adhering to SOLID principles. | Peer code reviews and static analysis checks. |

---

## 5. Success Criteria & Project Milestones
The project will be considered successful when:
1. An API client with a configured policy of 10 requests per minute is permitted exactly 10 requests, and the 11th request receives HTTP 429 with appropriate `Retry-After` headers.
2. Once the window resets or tokens refill, subsequent requests are immediately allowed.
3. Every request outcome (allowed, rejected, authenticated caller, latency) is legibly recorded in PostgreSQL.
4. The React dashboard displays aggregated traffic charts and active key statistics accurately reflecting backend records.

