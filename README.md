# Throttlix — Intelligent API Traffic Management Platform

[![Java Version](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Servlet API](https://img.shields.io/badge/Jakarta%20Servlet-6.0-blue.svg)](https://jakarta.ee/)
[![Database](https://img.shields.io/badge/MySQL-8.x-blue.svg)](https://www.mysql.com/)
[![Container](https://img.shields.io/badge/Apache%20Tomcat-10.1%2B-yellow.svg)](https://tomcat.apache.org/)
[![Frontend](https://img.shields.io/badge/React-19%20%7C%20TypeScript-cyan.svg)](https://react.dev/)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**Throttlix** is an intelligent, high-throughput API traffic management and rate-limiting gateway designed to sit between external client applications and protected downstream backend services. It validates caller identity via API keys, enforces thread-safe rate-limiting policies (Token Bucket and Sliding Window), permits allowed traffic, rejects excess requests with HTTP `429 Too Many Requests`, and records comprehensive telemetry for operational observability.

---

## 1. Problem Being Solved
When APIs experience sudden traffic bursts—such as flash sales, automated scraping bots, or client-side retry loops—downstream backend services become overwhelmed:
- Server resources (CPU, memory, threads, database connection pools) become exhausted, resulting in degraded latency or cascading outages.
- A single aggressive or malfunctioning consumer consumes an unfair share of system capacity, starving legitimate users.
- Engineering teams lack real-time visibility into which API keys or endpoints are generating excessive traffic.
- Rate limits hardcoded into backend business endpoints are rigid and difficult to update.

**Throttlix** solves this by establishing a lightweight, deterministic traffic-control gateway at the perimeter that enforces usage quotas fairly, transparently, and observably before backend work begins.

---

## 2. Key Features

- **API Key Lifecycle Management:** Generation, secure SHA-256 hash persistence, and instant revocation of client API credentials.
- **Header-Based Authentication:** High-performance validation of `X-API-Key` headers on incoming HTTP requests.
- **Concurrency-Safe Rate Limiting:**
  - **Token Bucket Algorithm:** Accommodates short bursts while enforcing steady-state request velocity.
  - **Sliding Window Algorithm:** Tracks rolling request timestamps to eliminate fixed-window boundary burst exploits.
- **Configurable Dynamic Policies:** Associate custom request limits, time windows, and algorithm selections with specific clients.
- **Deterministic HTTP 429 & Telemetry Headers:** Returns RFC-compliant rate headers (`X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`).
- **MySQL Telemetry Logging:** Records every request outcome (status, route, latency, allowed flag) via raw JDBC without exposing raw secrets.
- **Interactive React Dashboard:** Real-time visualization of traffic volume, allowed vs. rejected distributions, active keys, and latency graphs.

---

## 3. Technology Stack

| Layer | Technologies | Purpose |
| :--- | :--- | :--- |
| **Backend Core** | Java 21 LTS | High-performance core runtime and concurrency primitives. |
| **Web Layer** | Jakarta Servlet API 6.0 | RESTful request/response handling and filter interception. |
| **Servlet Container** | Apache Tomcat 10.1+ | Jakarta EE 10 compliant servlet execution runtime. |
| **Build Tool** | Apache Maven 3.9+ | Standard dependency management and WAR packaging. |
| **Database** | MySQL 8.x & JDBC | Durable persistence of clients, keys, policies, and audit logs. |
| **JSON Processing** | Jackson | High-throughput JSON serialization/deserialization. |
| **Testing** | JUnit 5 & Mockito | Unit, concurrency, and isolated service testing. |
| **Frontend UI** | React 19, TypeScript, Vite | Modern, typed Single Page Application (SPA) dashboard. |
| **Styling & Charts** | Tailwind CSS v4, Recharts | Utility-first responsive design and dynamic analytics charts. |

> **Architectural Note:** Throttlix deliberately uses **Core Java, Jakarta Servlets, and native JDBC**. It does **NOT** use Spring Boot, Spring MVC, Hibernate, or JPA.

---

## 4. System Architecture Overview

```mermaid
flowchart LR
    Client[API Consumer / Postman] -->|HTTP + X-API-Key| Gateway[Throttlix Gateway\nApache Tomcat 10.1+]
    DashboardUser[Operator / Browser] -->|React Dashboard| DashboardAPI[Analytics Servlets]
    
    Gateway --> Auth[Auth & Validation Service]
    Gateway --> Engine[Rate Limiting Engine]
    Engine --> TB[Token Bucket Strategy]
    Engine --> SW[Sliding Window Strategy]
    
    Gateway --> Log[Telemetry Logging]
    
    Auth --> Repo[JDBC DAO Layer]
    Engine --> Repo
    Log --> Repo
    DashboardAPI --> Repo
    
    Repo --> DB[(MySQL 8.x)]
    
    Gateway -->|Allowed Request| ProtectedBackend[Protected Backend APIs]
    Gateway -->|Rejected (HTTP 429)| Client
```

---

## 5. Prerequisites & Environment Setup

Ensure the following tools are installed:
- **JDK 21 LTS** (`JAVA_HOME` configured)
- **Apache Maven 3.9+**
- **Apache Tomcat 10.1+** (`CATALINA_HOME` configured)
- **MySQL 8.x** (running on port `3306`)
- **Node.js 20+** and **npm 10+**

Detailed setup instructions: [docs/06-deployment/ENVIRONMENT_SETUP.md](docs/06-deployment/ENVIRONMENT_SETUP.md)

---

## 6. Installation & How to Run

### 6.1 Database Initialization
```bash
# Connect to MySQL and execute schema script
mysql -u root -p < database/mysql-schema.sql
```

### 6.2 Backend Build & Deployment
```bash
# Navigate to backend directory
cd backend

# Build and package WAR archive
mvn clean package

# Deploy generated WAR to Apache Tomcat
cp target/throttlix-backend.war $CATALINA_HOME/webapps/

# Start Tomcat (or run via your IDE)
$CATALINA_HOME/bin/startup.sh    # On Linux / macOS
%CATALINA_HOME%\bin\startup.bat # On Windows
```
Verify backend health: `curl http://localhost:8080/throttlix-backend/api/health`

### 6.3 Frontend Dashboard Execution
```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start local development server
npm run dev
```
Access the dashboard at `http://localhost:5173`.

---

## 7. Repository Structure

```text
Throttlix/
├── backend/                        # Java 21 Jakarta Servlet application
│   ├── src/main/java/com/throttlix/
│   │   ├── config/                 # DB configuration & connection management
│   │   ├── controller/             # Servlets & HTTP endpoints
│   │   ├── dao/                    # JDBC data access objects
│   │   ├── filter/                 # Ingress gateway filters
│   │   ├── model/                  # Entities & DTOs
│   │   ├── ratelimiter/            # Rate limiting strategies & engine
│   │   ├── service/                # Business & orchestration logic
│   │   └── util/                   # Reusable helpers & hashing utilities
│   ├── src/main/resources/         # application.properties
│   ├── src/main/webapp/WEB-INF/    # web.xml deployment descriptor
│   └── pom.xml                     # Maven WAR configuration
├── frontend/                       # React 19 + TypeScript Vite dashboard
├── database/                       # Database schemas (MySQL 8.x active & PostgreSQL history)
│   ├── mysql-schema.sql            # Active MySQL 8.x schema
│   └── schema.sql                  # Historical PostgreSQL migration
├── docs/                           # Comprehensive Engineering Documentation
│   ├── 01-project-discovery/       # Overview, problem statement, objectives, scope
│   ├── 02-requirements/            # SRS, functional, non-functional, roles, use cases
│   ├── 03-system-design/           # Architecture, modules, DB design, APIs, data flow, UI
│   ├── 04-development/             # Development plan, coding standards, git workflow, DoD
│   ├── 05-testing/                 # Testing strategy, test cases, bug reporting
│   ├── 06-deployment/              # Deployment guide, environment setup, release management
│   └── 07-project-management/      # Team roles, meeting notes, ADRs, risk register, roadmap
├── .github/                        # GitHub PR and Issue templates
├── CONTRIBUTING.md                  # Contributor guidelines
├── CHANGELOG.md                     # Version changelog
├── LICENSE                          # MIT License
├── .gitignore
└── README.md
```

---

## 8. Complete Documentation Library

| Category | Document | Description |
| :--- | :--- | :--- |
| **01. Discovery** | [PROJECT_OVERVIEW.md](docs/01-project-discovery/PROJECT_OVERVIEW.md) | Vision, motivation, solution summary, and expected outcomes. |
| | [PROBLEM_STATEMENT.md](docs/01-project-discovery/PROBLEM_STATEMENT.md) | Detailed analysis of failure modes in unthrottled architectures. |
| | [PROJECT_OBJECTIVES.md](docs/01-project-discovery/PROJECT_OBJECTIVES.md) | Business, technical, and quality metrics. |
| | [PROJECT_SCOPE.md](docs/01-project-discovery/PROJECT_SCOPE.md) | Scope matrix (MVP Core, Secondary, Future Enhancements, Out of Scope). |
| **02. Requirements** | [SRS.md](docs/02-requirements/SRS.md) | Full IEEE-inspired Software Requirements Specification. |
| | [FUNCTIONAL_REQUIREMENTS.md](docs/02-requirements/FUNCTIONAL_REQUIREMENTS.md) | Detailed requirements (FR-01 through FR-16). |
| | [NON_FUNCTIONAL_REQUIREMENTS.md](docs/02-requirements/NON_FUNCTIONAL_REQUIREMENTS.md) | Latency, concurrency, security, and reliability constraints. |
| | [USER_ROLES.md](docs/02-requirements/USER_ROLES.md) | Administrator, Consumer, and Auditor role profiles. |
| | [USE_CASES.md](docs/02-requirements/USE_CASES.md) | Standardized use case specifications (UC-01 through UC-06). |
| **03. System Design** | [SYSTEM_ARCHITECTURE.md](docs/03-system-design/SYSTEM_ARCHITECTURE.md) | Gateway patterns, layered architecture, and Mermaid diagrams. |
| | [MODULE_BREAKDOWN.md](docs/03-system-design/MODULE_BREAKDOWN.md) | Subsystem responsibilities, contracts, inputs, outputs, and ownership. |
| | [DATABASE_DESIGN.md](docs/03-system-design/DATABASE_DESIGN.md) | Relational schema, ER diagram, normalization, and indexes. |
| | [API_DOCUMENTATION.md](docs/03-system-design/API_DOCUMENTATION.md) | REST API endpoints, request bodies, responses, and headers. |
| | [DATA_FLOW.md](docs/03-system-design/DATA_FLOW.md) | Ingress request lifecycle and analytics query data paths. |
| | [UI_UX_DESIGN.md](docs/03-system-design/UI_UX_DESIGN.md) | React dashboard layout, metrics bar, and Recharts specs. |
| **04. Development** | [DEVELOPMENT_PLAN.md](docs/04-development/DEVELOPMENT_PLAN.md) | Milestone roadmap (M1 to M6) and delivery timelines. |
| | [CODING_STANDARDS.md](docs/04-development/CODING_STANDARDS.md) | Java conventions, SOLID principles, and thread-safety rules. |
| | [GIT_WORKFLOW.md](docs/04-development/GIT_WORKFLOW.md) | Branching strategy, Conventional Commits, and PR lifecycle. |
| | [TASK_ALLOCATION.md](docs/04-development/TASK_ALLOCATION.md) | Work Breakdown Structure (WBS) with task priorities. |
| | [DEFINITION_OF_DONE.md](docs/04-development/DEFINITION_OF_DONE.md) | Mandatory quality gates before merging code to `main`. |
| **05. Testing** | [TESTING_STRATEGY.md](docs/05-testing/TESTING_STRATEGY.md) | Test pyramid, concurrency stress tests, and tools. |
| | [TEST_CASES.md](docs/05-testing/TEST_CASES.md) | Detailed verification scenarios (TC-01 through TC-07). |
| | [BUG_REPORTING.md](docs/05-testing/BUG_REPORTING.md) | Defect severity matrix, triage process, and issue template. |
| **06. Deployment** | [DEPLOYMENT.md](docs/06-deployment/DEPLOYMENT.md) | Step-by-step Tomcat WAR deployment and frontend hosting. |
| | [ENVIRONMENT_SETUP.md](docs/06-deployment/ENVIRONMENT_SETUP.md) | Prerequisites and local workstation configuration. |
| | [RELEASE_MANAGEMENT.md](docs/06-deployment/RELEASE_MANAGEMENT.md) | Semantic versioning, pre-release checklist, and Git tagging. |
| **07. Management** | [TEAM_ROLES.md](docs/07-project-management/TEAM_ROLES.md) | Functional role assignments and RACI matrix. |
| | [MEETING_NOTES.md](docs/07-project-management/MEETING_NOTES.md) | Meeting logs, sprint syncs, and architecture notes. |
| | [DECISION_LOG.md](docs/07-project-management/DECISION_LOG.md) | Architecture Decision Records (ADR-001 through ADR-004). |
| | [RISK_REGISTER.md](docs/07-project-management/RISK_REGISTER.md) | Risk assessment matrix and mitigation strategies. |
| | [PROJECT_ROADMAP.md](docs/07-project-management/PROJECT_ROADMAP.md) | Visual 4-week timeline covering Milestones M1 to M6. |

---

## 9. Team Information
- **Lead Software Architect & Developer:** Prachi Bhardwaj
- **Contributors & Roles:** Detailed in [TEAM_ROLES.md](docs/07-project-management/TEAM_ROLES.md) (`[TO BE CONFIRMED]`)

---

## 10. License
This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
