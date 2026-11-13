# Risk Register & Mitigation Strategy — Throttlix

## 1. Overview
This document identifies potential technical, operational, and project risks for **Throttlix**, evaluates their probability and impact, and defines actionable mitigation strategies.

---

## 2. Risk Evaluation Matrix

| Risk ID | Category | Description | Probability | Impact | Mitigation Strategy | Contingency Plan | Status |
| :---: | :---: | :--- | :---: | :---: | :--- | :--- | :---: |
| **RSK-01** | Technical | **Concurrency Race Conditions:** Simultaneous requests consume more than the allowed token capacity. | Medium | High | Use atomic operations (`AtomicLong`, `AtomicInteger`) and thread-safe data structures (`ConcurrentHashMap`). Enforce multi-threaded concurrency unit tests. | Add explicit synchronization block around counter evaluation. | Open |
| **RSK-02** | Technical | **Database Connection Starvation:** High request volume saturates JDBC connection pool during telemetry logging. | Medium | High | Decouple telemetry logging from the critical request path using an asynchronous worker queue or background thread pool. Optimize query indexes. | Temporarily drop non-critical audit rows or buffer in memory during extreme traffic spikes. | Open |
| **RSK-03** | Scope | **Scope Creep (Redis Premature Optimization):** Attempting distributed Redis integration before single-instance algorithms are verified. | Medium | High | Strictly enforce the Milestone plan (ADR-003). Deliver and test single-instance Token Bucket in Java memory before touching Redis. | Postpone Redis integration entirely to Phase 2 if Week 3 timeline slips. | Open |
| **RSK-04** | Security | **Plaintext Credential Exposure:** Raw API key secrets leaked into log files, error responses, or Git commits. | Low | Critical | Compute SHA-256 hash immediately upon receipt; display raw secret once at creation; enforce code review checks for logging sanitization. | Immediately revoke compromised key via administrative patch endpoint. | Open |
| **RSK-05** | Technical | **Timestamp Drift in Sliding Window:** Inaccurate system clocks or non-monotonic timestamps impair sliding window cleanup. | Low | Medium | Use monotonic high-precision system time (`System.currentTimeMillis()`) and ensure test environments synchronize clocks via NTP. | Fall back to Token Bucket policy if sliding window fails edge tests. | Open |
| **RSK-06** | Operational | **Tomcat Container Misconfiguration:** Incompatibility with Jakarta EE 10 / Servlet 6.0 namespaces on older Tomcat versions. | Low | High | Standardize deployment documentation explicitly requiring Apache Tomcat 10.1+. Provide setup verification tests (`/api/health`). | Provide pre-configured Tomcat archive in developer documentation. | Closed |
