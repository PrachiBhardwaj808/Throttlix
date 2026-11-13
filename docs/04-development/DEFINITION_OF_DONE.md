# Definition of Done (DoD) — Throttlix

## 1. Overview
The **Definition of Done (DoD)** establishes the mandatory quality criteria that every feature, pull request, and milestone deliverable must fulfill before it can be merged into `main` and accepted as complete.

---

## 2. Universal Quality Gates

Every pull request must pass the following checks:

### 2.1 Code Quality & Compilation
- [ ] Code compiles cleanly on Java 21 LTS with zero compiler errors.
- [ ] Frontend builds cleanly with zero TypeScript errors (`tsc -b && vite build`).
- [ ] Code strictly follows [CODING_STANDARDS.md](CODING_STANDARDS.md) (naming conventions, package structures, SOLID design).
- [ ] No unapproved frameworks (Spring Boot, Hibernate, JPA) are present in `backend/pom.xml`.

### 2.2 Testing & Concurrency Verification
- [ ] All new and existing automated unit tests pass via Maven (`mvn clean test`).
- [ ] Critical algorithm components (`TokenBucketStrategy`, `SlidingWindowStrategy`) include dedicated multi-threaded concurrency tests verifying zero over-consumption under parallel execution.
- [ ] Unit test coverage on core service and algorithm packages achieves $\ge 85\%$.
- [ ] Boundary conditions (empty bucket, window rollover, null headers, expired keys) are explicitly tested.

### 2.3 Security & Secret Management
- [ ] Zero raw API key secrets, passwords, or credentials are committed to Git, written to PostgreSQL, or logged.
- [ ] All database queries strictly utilize JDBC `PreparedStatement` with bind variables (`?`).
- [ ] External HTTP error responses return generic, clean JSON error messages without database exceptions or JVM stack traces.

### 2.4 Architecture & Interface Alignment
- [ ] Layer boundaries (Servlet $\rightarrow$ Service $\rightarrow$ DAO $\rightarrow$ Model) are strictly maintained without cross-layer leaks.
- [ ] HTTP status codes align with [API_DOCUMENTATION.md](../03-system-design/API_DOCUMENTATION.md) (`200`, `201`, `400`, `401`, `403`, `429`, `500`).
- [ ] Rate-limit headers (`X-RateLimit-*`, `Retry-After`) are formatted and populated accurately.

### 2.5 Documentation & Review
- [ ] Relevant documentation files in `docs/` are updated to reflect any API, schema, or configuration modifications.
- [ ] Pull request has been reviewed and approved by at least one core team member.
- [ ] Branch is rebased cleanly on latest `main` with no merge conflicts.
