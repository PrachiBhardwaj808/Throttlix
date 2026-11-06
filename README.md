# Throttlix — Intelligent API Traffic Management and Rate Limiting Platform

Throttlix is a high-performance API traffic management and rate limiting platform designed to protect backend microservices and APIs from abusive traffic, sudden load spikes, and quota exhaustion.

Built using core Java and Jakarta Servlets without heavyweight application frameworks, Throttlix delivers low-latency request inspection, policy enforcement (Token Bucket, Sliding Window), and API metrics observability.

---

## 1. Technology Stack

### Backend
- **Java 21** (LTS)
- **Jakarta Servlets 6.0** (Tomcat 10.1+)
- **JDBC** (Native PostgreSQL connectivity)
- **Apache Tomcat 10.1+** (Servlet container)
- **Apache Maven** (Build automation and dependency management)
- **JUnit 5 & Mockito** (Unit and integration testing)

### Database
- **PostgreSQL 15+** (Persistent API keys, client profiles, and traffic metrics)

### Frontend
- **React 19**
- **TypeScript**
- **Vite** (Next-generation frontend tooling)
- **Tailwind CSS** (Utility-first styling)
- **Axios** (HTTP client for API communication)
- **Recharts** (Interactive metrics and traffic visualization)

### Future Enhancements
- **Redis** (Distributed in-memory rate limiting counters)
- **Docker & Docker Compose** (Containerized orchestration)

---

## 2. Project Architecture & Structure

```text
Throttlix/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/throttlix/
│   │   │   │   ├── controller/     # HTTP servlets and request dispatching
│   │   │   │   ├── service/        # Core business and orchestration logic
│   │   │   │   ├── dao/            # Database access layer using raw JDBC
│   │   │   │   ├── model/          # Entities, POJOs, and DTOs
│   │   │   │   ├── filter/         # Servlet filters for auth & rate limiting
│   │   │   │   ├── ratelimiter/    # Rate limiting algorithms (Token Bucket, Sliding Window)
│   │   │   │   ├── config/         # System & database configuration loader
│   │   │   │   └── util/           # Reusable helpers & common utilities
│   │   │   ├── resources/          # Configuration files (application.properties)
│   │   │   └── webapp/WEB-INF/     # Deployment descriptor (web.xml)
│   │   └── test/java/com/throttlix/ # Automated tests with JUnit 5 & Mockito
│   └── pom.xml                     # Maven configuration (WAR packaging, Java 21)
├── frontend/                       # React + TypeScript SPA powered by Vite & Tailwind CSS
├── database/                       # PostgreSQL schema migrations and DDL scripts
│   └── schema.sql
├── docs/                           # Architectural and design documentation
│   ├── architecture.md
│   ├── database-design.md
│   └── api-documentation.md
├── .gitignore
└── README.md
```

---

## 3. Prerequisites

Ensure the following tools are installed in your environment:
- **Java Development Kit (JDK) 21**
- **Apache Maven 3.9+**
- **Apache Tomcat 10.1+** (Jakarta EE 10 compatible)
- **PostgreSQL 15+**
- **Node.js 20+** and **npm 10+**

---

## 4. Getting Started

### Backend Setup
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Build the WAR package:
   ```bash
   mvn clean package
   ```
3. Deploy the generated `target/throttlix.war` to your Apache Tomcat 10.1+ `webapps/` folder, or configure your IDE's Tomcat runner to point to the backend exploded artifact.

### Frontend Setup
1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the local Vite development server:
   ```bash
   npm run dev
   ```
   The application dashboard will be accessible at `http://localhost:5173`.
