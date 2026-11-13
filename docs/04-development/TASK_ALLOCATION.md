# Task Allocation & Work Breakdown — Throttlix

## 1. Overview
This document outlines the detailed work packages, task allocations, priority rankings, dependencies, and ownership assignments across all milestones for **Throttlix**.

---

## 2. Work Breakdown Structure (WBS)

| Task ID | Milestone | Task Description & Deliverable | Priority | Dependencies | Assigned Owner | Status |
| :---: | :---: | :--- | :---: | :---: | :---: | :---: |
| **TSK-01** | M1 | Repository setup, Maven POM, WAR configuration, and folder layout. | High | None | `[TO BE CONFIRMED]` | Completed |
| **TSK-02** | M1 | PostgreSQL DDL schema definition (`database/schema.sql`). | High | TSK-01 | `[TO BE CONFIRMED]` | Completed |
| **TSK-03** | M1 | Backend Health Servlet & Tomcat deployment verification. | High | TSK-01 | `[TO BE CONFIRMED]` | Completed |
| **TSK-04** | M2 | JDBC Connection Manager & DataSource utility (`DatabaseConfig`). | High | TSK-02 | `[TO BE CONFIRMED]` | Planned |
| **TSK-05** | M2 | `ClientDAO` and `ApiKeyDAO` implementation with SHA-256 hashing. | High | TSK-04 | `[TO BE CONFIRMED]` | Planned |
| **TSK-06** | M2 | `PolicyDAO` implementation & client-policy junction logic. | High | TSK-04 | `[TO BE CONFIRMED]` | Planned |
| **TSK-07** | M2 | Admin REST Servlets for Client & Policy creation. | Medium | TSK-05, TSK-06 | `[TO BE CONFIRMED]` | Planned |
| **TSK-08** | M3 | `RateLimitStrategy` interface and `RateLimitDecision` domain model. | High | TSK-01 | `[TO BE CONFIRMED]` | Planned |
| **TSK-09** | M3 | `TokenBucketStrategy` with continuous refill and atomic token decrement. | High | TSK-08 | `[TO BE CONFIRMED]` | Planned |
| **TSK-10** | M3 | `SlidingWindowStrategy` with timestamp deque and rolling window purge. | High | TSK-08 | `[TO BE CONFIRMED]` | Planned |
| **TSK-11** | M3 | Multi-threaded concurrency unit test suite for algorithm strategies. | High | TSK-09, TSK-10 | `[TO BE CONFIRMED]` | Planned |
| **TSK-12** | M4 | Ingress Gateway Servlet & request validation pipeline. | High | TSK-05, TSK-09 | `[TO BE CONFIRMED]` | Planned |
| **TSK-13** | M4 | Downstream backend HTTP forwarding logic & response proxying. | High | TSK-12 | `[TO BE CONFIRMED]` | Planned |
| **TSK-14** | M4 | Rate-limit response headers builder (`X-RateLimit-*`, `Retry-After`). | High | TSK-12 | `[TO BE CONFIRMED]` | Planned |
| **TSK-15** | M4 | Asynchronous telemetry logging service and `RequestLogDAO`. | Medium | TSK-04, TSK-12 | `[TO BE CONFIRMED]` | Planned |
| **TSK-16** | M4 | Demonstration protected endpoint (`/api/v1/demo/orders`). | Medium | TSK-13, TSK-14 | `[TO BE CONFIRMED]` | Planned |
| **TSK-17** | M5 | React Frontend setup with Tailwind CSS, Axios, and Recharts. | Medium | TSK-01 | `[TO BE CONFIRMED]` | Completed |
| **TSK-18** | M5 | Backend Analytics Aggregation API (`/api/v1/analytics/summary`). | Medium | TSK-15 | `[TO BE CONFIRMED]` | Planned |
| **TSK-19** | M5 | Dashboard KPI Summary Cards & Time-Series Chart component. | Medium | TSK-17, TSK-18 | `[TO BE CONFIRMED]` | Planned |
| **TSK-20** | M5 | Client Usage and Ranking table component. | Low | TSK-19 | `[TO BE CONFIRMED]` | Planned |
| **TSK-21** | M6 | End-to-end integration test suite using Postman. | High | TSK-16, TSK-18 | `[TO BE CONFIRMED]` | Planned |
| **TSK-22** | M6 | Concurrency & burst load testing using JMeter / k6. | High | TSK-16 | `[TO BE CONFIRMED]` | Planned |
| **TSK-23** | M6 | Final documentation review, presentation slides, and demo script. | High | All | `[TO BE CONFIRMED]` | Planned |
