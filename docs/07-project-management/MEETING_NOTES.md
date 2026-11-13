# Meeting Notes & Architecture Sessions — Throttlix

## 1. Overview
This log captures all formal team syncs, architecture discussions, sprint planning meetings, and decision reviews for **Throttlix**.

---

## 2. Meeting Log

### Session 01: Project Discovery & Architecture Baseline
- **Date:** 2026-11-06
- **Attendees:** Full Team (`[TO BE CONFIRMED]`)
- **Agenda:** Project scope clarification, technology stack decisions, and framework restrictions.
- **Key Discussion Points:**
  1. Confirmed core problem: Traffic surges exhaust backend resources; need perimeter rate limiting before backend execution.
  2. Confirmed framework constraint: Strictly **NO Spring Boot**. Use **Java 21**, **Jakarta Servlets 6.0**, **Apache Tomcat 10.1+**, and **raw JDBC**.
  3. Scope boundary: Single-instance in-memory limiter for MVP (Token Bucket + Sliding Window); Redis shared counters deferred to post-MVP.
- **Decisions Made:**
  - Implemented ADR-001 (Core Java + Servlets over Spring Boot).
  - Implemented ADR-002 (Raw JDBC over ORM).
- **Action Items:**
  - [x] Initialize Git repository, package hierarchy, and Maven build setup.
  - [x] Author comprehensive project documentation suite.

---

### Session 02: Sprint 1 Planning (Foundation & Identity)
- **Date:** `[TO BE CONFIRMED]`
- **Attendees:** `[TO BE CONFIRMED]`
- **Agenda:** Task allocation for Milestones M1 and M2.
- **Key Discussion Points:**
  - `[TO BE CONFIRMED]`
- **Decisions Made:**
  - `[TO BE CONFIRMED]`
- **Action Items:**
  - [ ] Implement `DatabaseConfig` and connection pool.
  - [ ] Implement `ApiKeyDAO` and `PolicyDAO`.

---

## 3. Standard Meeting Template
```markdown
### Session XX: [Meeting Title]
- **Date:** YYYY-MM-DD
- **Attendees:** [List of participants]
- **Agenda:** [Key topics to discuss]
- **Key Discussion Points:**
  1. [Point 1]
  2. [Point 2]
- **Decisions Made:** [Decisions reached or ADRs created]
- **Action Items:**
  - [ ] [Task 1] (Owner: [Name], Due: [Date])
  - [ ] [Task 2] (Owner: [Name], Due: [Date])
```
