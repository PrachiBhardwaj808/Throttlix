# Problem Statement — Throttlix

## 1. Context & Operational Background
In distributed web environments, APIs are exposed to unpredictable and variable traffic loads. Downstream protected services have fixed processing capacities bounded by CPU cores, thread pool sizes, database connection pools, and memory constraints. 

When demand exceeds processing capacity, unconstrained request ingress leads to severe operational degradation.

---

## 2. Core Problem Analysis
The challenge is not merely high traffic volume; it is the **inability to control traffic fairly, predictably, and observably before backend compute begins**.

### 2.1 Key Operational Failure Modes

| Failure Mode | Root Cause | Impact on Unprotected System | How Throttlix Mitigates |
| :--- | :--- | :--- | :--- |
| **Resource Depletion** | High-concurrency traffic bursts (e.g., flash sales, scraping bots). | Thread pool starvation, socket exhaustion, database connection saturation, leading to cascading service outages. | Intercepts requests at the servlet gateway layer and drops excess load before backend execution. |
| **Noisy Neighbor Starvation** | A single consumer executes aggressive retry loops or unthrottled bulk calls. | Capacity is monopolized by one caller; legitimate callers experience degraded latency or timeouts. | Segregates traffic limits per client API key, ensuring isolated capacity allocations. |
| **Operational Blindness** | Request logs are scattered across backend servers without centralized rate telemetry. | Engineering teams cannot determine which consumer, key, or route triggered an overload event. | Persists structured audit and metrics records with client identifiers, status codes, and latencies. |
| **Unpredictable Client Experience** | Servers drop connections abruptly or return generic `500 Internal Server Error` or timeout responses. | Clients cannot distinguish between system outages and temporary quota exhaustion. | Returns standardized HTTP `429 Too Many Requests` responses with `Retry-After` and reset timestamps. |
| **Rigid Configuration Management** | Rate limits hardcoded into backend business services. | Updating thresholds requires code modification, testing cycles, and application redeployment. | Decouples policy definitions into PostgreSQL, allowing runtime policy assignment. |

---

## 3. Traffic Scenarios Requiring Governance
1. **Unintentional Client Defects:** Software integration bugs, such as unbounded recursive retry loops without exponential backoff.
2. **Scheduled Batch Processing:** Automated client scripts executing high-velocity queries during peak business hours.
3. **Malicious / Abusive Probing:** Bot networks scanning API endpoints or attempting credential stuffing.
4. **Legitimate Traffic Surges:** Seasonal sales events, marketing announcements, or breaking news events causing transient traffic spikes.

---

## 4. Architectural Necessity for an Intermediary Layer
Attempting to handle rate limiting inside deep business logic services causes:
- Unnecessary consumption of database and CPU resources executing authentication and ORM queries before discarding a request.
- Duplication of throttling logic across multiple heterogeneous microservices.
- Inconsistent client responses and non-standard header formats.

**Throttlix** solves this by establishing a dedicated, high-performance gateway boundary that enforces identity, validation, and traffic policy at the perimeter of the infrastructure.

