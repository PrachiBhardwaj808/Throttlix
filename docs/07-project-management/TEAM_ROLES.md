# Team Roles & Responsibilities — Throttlix

## 1. Overview
This document defines the team organization, functional roles, and responsibilities for the **Throttlix** project. 

In student, startup, and agile team settings, **multiple roles may be assigned to a single team member**.

---

## 2. Role Assignments & Ownership

| Functional Role | Primary Responsibilities | Assigned Team Member |
| :--- | :--- | :--- |
| **Project Manager (PM)** | Sprint planning, timeline tracking, milestone review, risk management, and GitHub milestone coordination. | `[TO BE CONFIRMED]` |
| **Product Owner (PO)** | Requirements prioritization, scope boundary governance, acceptance criteria validation, and stakeholder communication. | `[TO BE CONFIRMED]` |
| **Technical Lead / Architect** | System design, architectural integrity, code review standards, concurrency model review, and framework boundary enforcement. | `[TO BE CONFIRMED]` |
| **Backend Developer** | Java 21 servlets, request pipeline, algorithm strategies (`TokenBucket`, `SlidingWindow`), and Jackson JSON mapping. | `[TO BE CONFIRMED]` |
| **Database Developer** | PostgreSQL DDL schema maintenance, JDBC DAO query optimization, indexing strategy, and data integrity. | `[TO BE CONFIRMED]` |
| **Frontend Developer** | React 19 SPA development, Tailwind CSS responsive layouts, Recharts analytics integration, and API client hooks. | `[TO BE CONFIRMED]` |
| **QA / Test Engineer** | Automated JUnit 5 test suites, multi-threaded concurrency stress tests, Postman API collections, and load benchmarking. | `[TO BE CONFIRMED]` |

---

## 3. RACI Responsibility Assignment Matrix

| Work Item / Milestone | PM | PO | Tech Lead | Backend Dev | DB Dev | Frontend Dev | QA |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **M1: Repository & Base Setup** | A | C | R | R | R | R | I |
| **M2: Client & Policy DAOs** | I | C | A | R | R | I | C |
| **M3: Algorithm Strategies (Token Bucket)** | I | C | A | R | I | I | R |
| **M4: Ingress Request Path & Headers** | I | C | A | R | C | I | R |
| **M5: React Dashboard Integration** | I | C | A | C | I | R | R |
| **M6: Concurrency & Performance Testing** | A | I | C | C | C | I | R |

*Legend: **R** = Responsible, **A** = Accountable, **C** = Consulted, **I** = Informed.*
