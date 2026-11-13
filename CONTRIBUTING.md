# Contributing to Throttlix

Thank you for your interest in contributing to **Throttlix — Intelligent API Traffic Management Platform**.

To ensure a smooth collaborative workflow, please read and follow these contribution guidelines.

---

## 1. Code of Conduct

All contributors and maintainers are expected to collaborate professionally, respect diverse viewpoints, and provide constructive feedback during code reviews and discussions.

---

## 2. Branching Strategy & Git Workflow

- `main` is our production-ready, stable branch. All commits on `main` must pass all test suites and compile cleanly.
- Create feature or bugfix branches off `main` using descriptive naming:
  - `feature/<feature-name>` (e.g., `feature/token-bucket-algorithm`)
  - `fix/<bug-description>` (e.g., `fix/header-parsing-null-pointer`)
  - `docs/<doc-name>` (e.g., `docs/database-design-updates`)
- Write clear commit messages following Conventional Commits format:
  - `feat: add token bucket rate limiter strategy`
  - `fix: resolve race condition in token refill calculation`
  - `docs: update SRS acceptance criteria`
  - `test: add concurrent test cases for sliding window`

---

## 3. Development Guidelines

1. **Backend Standards:**
   - Java 21 LTS with Jakarta Servlet 6.0 and raw JDBC.
   - Do **NOT** introduce Spring Boot, Spring MVC, Hibernate, or other unapproved frameworks.
   - Maintain strict separation of concerns: Servlet -> Service -> DAO -> Database.
   - Write comprehensive JUnit 5 and Mockito tests for all business and algorithm logic.
2. **Frontend Standards:**
   - React 19 + TypeScript with Tailwind CSS and Vite.
   - Ensure zero TypeScript compiler errors (`npm run build`).
3. **Database Standards:**
   - PostgreSQL 15+.
   - All SQL queries must use `PreparedStatement` to prevent SQL injection.
   - Never log raw API key secrets; store and compare secure hashes only.

---

## 4. Pull Request Process

1. Ensure your branch is rebased on the latest `main`.
2. Run `mvn clean test` in `backend/` and `npm run build` in `frontend/` to confirm that all tests and builds pass.
3. Fill out the [Pull Request Template](.github/PULL_REQUEST_TEMPLATE.md) completely.
4. Request review from at least one core maintainer.
5. Address review feedback promptly before merging.

