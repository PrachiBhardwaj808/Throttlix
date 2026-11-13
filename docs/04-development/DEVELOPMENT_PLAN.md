# Development Plan & Milestone Roadmap — Throttlix

## 1. Plan Overview
The development plan for **Throttlix** is structured into six focused, sequential milestones designed to deliver an operational Minimum Viable Product (MVP) within a 4-week development cycle.

---

## 2. Milestone Breakdown

| Milestone ID & Name | Objectives & Core Technologies | Deliverables & Artifacts | Verification Checklist |
| :--- | :--- | :--- | :--- |
| **M1: Foundation & Setup** | Project setup, Java web app, Tomcat container runtime, PostgreSQL connection, and Git workflow. *(Java 21, Tomcat 10.1, JDBC, Maven)* | - Clean package hierarchy<br>- Database connection test (`HealthServlet`)<br>- Root documentation suite | [x] Maven WAR builds successfully<br>[x] PostgreSQL connectivity verified<br>[x] Git branching rules active |
| **M2: Identity & Policies** | Client entity models, API key generation/hashing, policy configuration, and authentication service. *(Servlets, JDBC, PostgreSQL)* | - `ApiKeyDAO`, `PolicyDAO`<br>- `AuthService`, `PolicyService`<br>- Endpoints `/api/v1/clients`, `/api/v1/policies` | [ ] SHA-256 key hashing verified<br>[ ] Unit tests for key lifecycle<br>[ ] Client-policy association functional |
| **M3: Rate Engines** | Concurrency-safe algorithm implementations for Token Bucket and Sliding Window. *(Java Concurrency, Atomics)* | - `RateLimitStrategy` interface<br>- `TokenBucketStrategy`<br>- `SlidingWindowStrategy` | [ ] Multi-threaded concurrency tests pass<br>[ ] Zero token double-spending under load<br>[ ] Unit test coverage > 85% |
| **M4: Ingress Request Path** | Full gateway request pipeline integration: validation, auth, rate limit, backend proxy, telemetry logging, headers. *(Servlets, Jackson, JDBC)* | - `RateLimitServlet` / Gateway filter<br>- Standard response headers generator<br>- Protected `/api/v1/demo/orders` endpoint | [ ] HTTP 429 returned on quota breach<br>[ ] Accurate `Retry-After` header verified<br>[ ] Database audit log row inserted |
| **M5: Analytics Dashboard** | Interactive web dashboard consuming aggregated backend telemetry. *(React 19, TypeScript, Tailwind, Recharts)* | - React SPA frontend<br>- KPI Summary cards<br>- Time-series volume chart<br>- Client usage ranking table | [ ] Frontend consumes `/analytics/summary`<br>[ ] Zero TypeScript build errors<br>[ ] Responsive UI across viewports |
| **M6: Hardening & Demo** | Comprehensive integration testing, performance benchmarking, documentation finalization, and demonstration script. *(Postman, JMeter/k6, GitHub)* | - Postman test collection<br>- Concurrency benchmark report<br>- Final release package & video script | [ ] End-to-end integration suite passes<br>[ ] Clean repository state on `main`<br>[ ] Complete demonstration recorded |

---

## 3. Dependency Graph Between Milestones

```mermaid
flowchart TD
    M1[M1: Foundation & Base Setup] --> M2[M2: Identity & Policies]
    M1 --> M3[M3: Rate Limiting Engines]
    M2 --> M4[M4: Ingress Request Pipeline]
    M3 --> M4
    M4 --> M5[M5: React Analytics Dashboard]
    M4 --> M6[M6: Hardening & Verification]
    M5 --> M6
```

---

## 4. Phase Schedule & Target Timelines

| Phase | Duration | Focus Area | Primary Deliverable |
| :---: | :---: | :--- | :--- |
| **Week 1** | Days 1–7 | M1 (Foundation) & M2 (Identity & Policies) | Functional Key Generation & PostgreSQL DAOs |
| **Week 2** | Days 8–14 | M3 (Rate Limiting Engines) | Concurrency-Safe Token Bucket & Sliding Window |
| **Week 3** | Days 15–21 | M4 (Gateway Request Path) & M5 (Frontend UI) | End-to-End Rate Limiting with React Dashboard |
| **Week 4** | Days 22–28 | M6 (Hardening, Load Testing & Demo) | Stress Tests, Postman Collection & Release Package |
