# Software Requirements Specification (SRS) — Throttlix

## 1. Introduction

### 1.1 Purpose
This Software Requirements Specification (SRS) document defines the complete functional, non-functional, and interface requirements for the **Throttlix Intelligent API Traffic Management Platform**. It serves as the authoritative technical baseline for engineering, quality assurance, and project management.

### 1.2 Document Conventions
- Requirements are uniquely identified using alphanumeric codes:
  - `FR-xx`: Functional Requirement
  - `NFR-xx`: Non-Functional Requirement
  - `UC-xx`: Use Case
- Priority levels: **Must Have (MVP)**, **Should Have (Secondary)**, **Could Have (Future)**.

### 1.3 Intended Audience
- Backend & Frontend Software Engineers
- Software Architects & Technical Leads
- Quality Assurance Engineers & Testers
- Project & Product Managers

### 1.4 Project Scope
Throttlix is an intermediary traffic management and API rate-limiting platform that intercepts incoming HTTP requests, validates API credentials, enforces token bucket and sliding window rate limits, rejects excess load with HTTP 429, forwards legitimate requests, and persists operational telemetry.

---

## 2. Overall Description

### 2.1 Product Perspective
Throttlix operates as an API Gateway and traffic-control perimeter situated between external API consumers (mobile apps, web clients, partner services) and internal protected microservices/endpoints.

```text
[External Clients / Postman]
             │ (HTTP + X-API-Key)
             ▼
┌────────────────────────────────────────────────────────┐
│               Throttlix API Gateway                    │
│  [Servlet Filter] -> [Auth Check] -> [Rate Limiter]    │
└────────────────────────────────────────────────────────┘
        │ Allowed                     │ Rejected (HTTP 429)
        ▼                             ▼
[Protected Backend APIs]        [Client Response + Headers]
        │                             │
        └──────────────┬──────────────┘
                       ▼
          [PostgreSQL Audit Logging]
                       │
                       ▼
          [React Analytics Dashboard]
```

### 2.2 Product Features Summary
- Client & API Key Management with cryptographic hash persistence.
- Header-based API Authentication (`X-API-Key`).
- Thread-safe Token Bucket rate-limiting algorithm.
- Sliding Window timestamp log rate-limiting algorithm.
- Dynamic per-client policy configuration.
- Standardized HTTP 429 and rate-limit telemetry headers (`X-RateLimit-*`, `Retry-After`).
- Low-latency PostgreSQL audit and request logging via raw JDBC.
- Interactive React dashboard for operational analytics.

### 2.3 User Classes and Characteristics
1. **API Administrator / Operator:** Configures clients, generates/revokes API keys, defines rate limit policies, and monitors traffic.
2. **API Consumer / Client Application:** Automated systems or external developers consuming protected endpoints authenticated by an API key.

### 2.4 Operating Environment
- **Server OS:** Cross-platform (Windows, Linux, macOS)
- **Runtime:** Java SE Development Kit (JDK) 21 LTS
- **Servlet Container:** Apache Tomcat 10.1+ (Jakarta EE 10 compliant)
- **Database:** PostgreSQL 15+
- **Frontend Environment:** Modern Web Browsers (Chrome 120+, Firefox 120+, Safari 17+, Edge)

### 2.5 Design & Implementation Constraints
- **Framework Constraint:** No Spring Framework (Spring Boot, Spring MVC, Spring Data). Core Java, Jakarta Servlets, and raw JDBC must be used.
- **Persistence Constraint:** No ORM (Hibernate/JPA). Queries must use explicit, parameterized SQL via JDBC `PreparedStatement`.
- **MVP State Constraint:** Counter state is maintained in-memory for single-instance deployment; Redis integration is reserved for post-MVP clustering.

### 2.6 Assumptions & Dependencies
- Downstream protected services are accessible via internal network HTTP connections.
- Client applications properly read standard HTTP response headers (`X-RateLimit-*`, `Retry-After`).

---

## 3. External Interface Requirements

### 3.1 User Interfaces
- A responsive Single Page Application (SPA) built using React 19, TypeScript, and Tailwind CSS.
- Views include: System Overview, Allowed vs. Rejected traffic charts, Client usage table, and Recent traffic stream.

### 3.2 Software & Protocol Interfaces
- **Communication Protocol:** HTTP/1.1 and HTTP/2 over TLS (HTTPS).
- **Data Interchange Format:** JSON (`application/json`) serialized via Jackson.
- **Authentication Interface:** HTTP Header `X-API-Key: <key-secret>`.

---

## 4. System Requirements & Traceability

### 4.1 Functional Requirements Summary
Detailed functional requirements are documented in [FUNCTIONAL_REQUIREMENTS.md](FUNCTIONAL_REQUIREMENTS.md):
- `FR-01`: API Key Lifecycle Management
- `FR-02`: API Authentication
- `FR-03`: Token Bucket Algorithm Implementation
- `FR-04`: Sliding Window Algorithm Implementation
- `FR-05`: Configurable Rate-Limit Policies
- `FR-06`: Request Validation & Sanitization
- `FR-07`: HTTP Response Standardization
- `FR-08`: Rate-Limit Telemetry Response Headers
- `FR-09`: Audit & Metrics Request Logging
- `FR-10`: Operator Analytics Dashboard
- `FR-11`: Client-Wise Usage Analytics
- `FR-12`: Real-Time Traffic Pattern Monitoring

### 4.2 Non-Functional Requirements Summary
Detailed non-functional requirements are documented in [NON_FUNCTIONAL_REQUIREMENTS.md](NON_FUNCTIONAL_REQUIREMENTS.md):
- `NFR-01`: Sub-5ms In-Memory Rate Evaluation Latency
- `NFR-02`: Concurrency Safety & Race Condition Immunity
- `NFR-03`: Zero Plaintext Secret Exposure
- `NFR-04`: Graceful Degradation & Controlled Failure Responses
- `NFR-05`: Modular Layered Architecture

---

## 5. Verification & Acceptance Criteria
1. When a client exceeds its configured capacity, Throttlix must reject subsequent calls with HTTP `429 Too Many Requests` in less than 5ms.
2. The `Retry-After` header must accurately reflect the refill duration or window expiration in seconds.
3. Database connection failures must return structured JSON `500 Internal Server Error` without crashing the Tomcat JVM or leaking SQL syntax.
4. Concurrency tests using parallel threads must prove that exactly $N$ requests are allowed when the limit is $N$, with zero over-allocation.

