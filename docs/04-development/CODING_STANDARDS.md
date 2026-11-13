# Coding Standards & Engineering Guidelines — Throttlix

## 1. Principles & Objectives
This document sets the engineering, code style, and architectural conventions for the **Throttlix** project. Consistent adherence guarantees code readability, maintainability, and security across the team.

---

## 2. Java Naming & Style Conventions

### 2.1 Package Naming
- All packages must be lowercase, under `com.throttlix.*`:
  - `com.throttlix.controller`: Servlets and HTTP request handlers.
  - `com.throttlix.service`: Core business logic and orchestration.
  - `com.throttlix.dao`: Database access objects using JDBC.
  - `com.throttlix.model`: Entities, DTOs, and value objects.
  - `com.throttlix.filter`: Servlet filters for request interception.
  - `com.throttlix.ratelimiter`: Rate limiting strategies and algorithm engines.
  - `com.throttlix.config`: Database connection pooling and system configuration.
  - `com.throttlix.util`: Common stateless helper utilities.

### 2.2 Class, Interface & Method Naming
- **Classes:** `UpperCamelCase` (e.g., `TokenBucketStrategy`, `ApiKeyDAO`, `RateLimitServlet`).
- **Interfaces:** `UpperCamelCase`, descriptive nouns or adjectives (e.g., `RateLimitStrategy`). Do not use Hungarian notation (`IRateLimitStrategy`).
- **Methods:** `lowerCamelCase`, starting with an active verb (e.g., `evaluateRequest()`, `hashApiKey()`, `findActivePolicy()`).
- **Constants:** `UPPER_SNAKE_CASE` (e.g., `DEFAULT_WINDOW_SECONDS`, `HEADER_API_KEY`).
- **Variables & Parameters:** `lowerCamelCase` with meaningful descriptive names (avoid single-letter names except loop counters).

---

## 3. Architectural & Design Rules (SOLID)
1. **Single Responsibility Principle (SRP):**
   - Servlets handle only HTTP mapping and response status codes.
   - Services execute only business logic.
   - DAOs execute only SQL queries and result mapping.
2. **Open/Closed Principle (OCP):**
   - Algorithms implement the `RateLimitStrategy` interface. New algorithms must be introduced by implementing this interface without altering the `RateLimitEngine` dispatcher.
3. **No Unapproved Frameworks:**
   - Do **NOT** introduce Spring Boot, Spring MVC, Spring Data, Hibernate, or JPA.
   - All persistence must use native JDBC with `PreparedStatement`.

---

## 4. Concurrency & Thread-Safety Rules
1. **Shared State Protection:**
   - Servlets are multi-threaded by default in Tomcat. Instance variables in servlets are shared among all concurrent requests.
   - Never store request-specific state in servlet instance fields.
   - In-memory rate-limiter counters must use `ConcurrentHashMap` and atomic primitives (`AtomicLong`, `AtomicInteger`) or explicit synchronized blocks.
2. **Deterministic Time Checks:**
   - Use `System.currentTimeMillis()` or `Instant.now()` for elapsed duration calculations.
   - Token refill updates must be atomic to prevent race conditions (two threads consuming the last remaining token).

---

## 5. Security & Secure Coding Practices
1. **SQL Injection Defense:**
   - Every SQL query must use `PreparedStatement` with bind variables (`?`).
   - String concatenation in SQL statements is strictly prohibited.
2. **Zero Plaintext Secrets:**
   - Raw API keys must never be logged, printed to console, or saved to the database.
   - Only store cryptographic hashes (SHA-256).
3. **Information Disclosure Prevention:**
   - Catch internal exceptions and output clean JSON error responses (`{"error": "...", "message": "..."}`).
   - Never expose database schemas, table names, or raw stack traces in HTTP responses.

---

## 6. Exception Handling & Logging
1. **Custom Exceptions:**
   - Use specific domain exceptions: `AuthenticationException`, `RateLimitExceededException`, `PolicyNotFoundException`.
2. **Catch Specific Exceptions:**
   - Catch specific exceptions (`SQLException`, `IOException`) rather than generic `Exception` or `Throwable`.
3. **Logging:**
   - Use standard Java logging (`java.util.logging` or SLF4J).
   - Log at appropriate levels: `INFO` for lifecycle events, `WARNING` for client 4xx rejections, `SEVERE` for system errors.
