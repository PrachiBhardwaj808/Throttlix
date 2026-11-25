# Data Flow & Execution Pipelines — Throttlix

## 1. Overview
This document traces the path of data across all components of **Throttlix**, detailing ingress payload transformation, internal service interaction, state updates, and telemetry persistence.

---

## 2. Ingress Traffic Pipeline

```mermaid
sequenceDiagram
    autonumber
    participant C as External Client / App
    participant S as Servlet / Filter Pipeline
    participant A as AuthService
    participant D as MySQL
    participant R as RateLimitEngine
    participant B as Protected Backend API

    C->>S: 1. HTTP Request (X-API-Key: tk_live_...)
    Note over S: Extract header, parse method & URI
    S->>A: 2. Authenticate (rawKey)
    A->>A: Compute SHA-256(rawKey)
    A->>D: 3. Query SELECT WHERE key_hash = ?
    D-->>A: Return Client & Policy ID
    A-->>S: Validated ClientContext
    
    S->>R: 4. Evaluate(clientKey, Policy)
    alt Tokens Available (Token Bucket)
        R->>R: Decrement token count atomically
        R-->>S: Decision: ALLOW (remaining = 7)
        S->>B: 5. Forward request to backend
        B-->>S: Backend 200 OK + Payload
        S->>D: 6. INSERT INTO request_logs (status=200, allowed=true)
        S-->>C: 7. HTTP 200 OK + Rate-Limit Headers + Body
    else Bucket Empty / Window Exhausted
        R-->>S: Decision: REJECT (retryAfter = 2s)
        S->>D: 8. INSERT INTO request_logs (status=429, allowed=false)
        S-->>C: 9. HTTP 429 Too Many Requests + Retry-After
    end
```

---

## 3. Data Transformation at Each Boundary

| Pipeline Stage | Input Data Format | Output Data Format | Transformation Logic |
| :--- | :--- | :--- | :--- |
| **Ingress Header Extraction** | Raw HTTP Request stream | Extracted `X-API-Key` string | Null-check and alphanumeric character validation. |
| **Authentication Hashing** | Plaintext Key (e.g. `tk_live_...`) | 64-character Hex SHA-256 Digest | `MessageDigest.getInstance("SHA-256")`. |
| **Policy Resolution** | Client ID integer | `RateLimitPolicy` Java Object | Cache lookup or SQL read from `rate_limit_policies`. |
| **Algorithm Evaluation** | `(storedTokens, lastRefillTimestamp)` | `(newTokens, updatedTimestamp)` | Continuous refill calculation and atomic decrement. |
| **Telemetry Persistence** | `RequestLogRecord` Object | Parameterized SQL `INSERT` statement | Map attributes to `PreparedStatement` parameters. |
| **Client Response Formatting** | Backend Payload + Limiter State | HTTP Response + Standard Headers | Set `X-RateLimit-*` and `Retry-After` headers. |

---

## 4. Analytics Data Flow (Dashboard Consumption)

```mermaid
sequenceDiagram
    autonumber
    participant User as System Operator
    participant UI as React Dashboard
    participant API as Analytics Servlet
    participant DB as MySQL Database

    User->>UI: Opens Dashboard
    UI->>API: GET /api/v1/analytics/summary
    API->>DB: Execute Aggregation Query (COUNT, AVG latency, GROUP BY allowed)
    DB-->>API: Result set rows
    API->>API: Serialize to JSON (AnalyticsSummaryDTO)
    API-->>UI: JSON Response Payload
    UI->>UI: Update React state & render Recharts graphs
    UI-->>User: Visual cards, charts, and client table
```
