# Git Workflow & Collaboration Guidelines — Throttlix

## 1. Overview
This document outlines the version control strategy, branching workflow, commit message standards, and pull request procedures for the **Throttlix** development team.

---

## 2. Branching Strategy

```text
main (Production-ready, always releasable)
  ▲
  │ Pull Request (Review + Passing CI/Tests)
  │
feature/token-bucket-engine ───■───■───■ (Feature development)
fix/database-connection-leak ───■───■    (Bug fixes)
docs/api-documentation-updates ──■       (Documentation updates)
```

### 2.1 Branch Types & Naming Conventions
- **`main`:** The primary trunk branch. Must compile cleanly and pass all automated tests at all times. Direct pushes to `main` without review are prohibited in multi-contributor workflows.
- **`feature/<name>`:** For all new features or milestone implementations.
  - Examples: `feature/token-bucket-engine`, `feature/api-key-dao`, `feature/react-dashboard-kpi`
- **`fix/<name>`:** For bug fixes and defect resolution.
  - Examples: `fix/token-refill-math`, `fix/null-pointer-header-check`
- **`docs/<name>`:** For documentation updates and architecture changes.
  - Examples: `docs/database-design-updates`, `docs/srs-additions`

---

## 3. Commit Message Standards (Conventional Commits)
All commit messages must adhere to the Conventional Commits specification:

$$\text{type}(\text{optional-scope}):\ \text{short description in present tense}$$

### Permitted Commit Types:
- **`feat:`** A new feature or capability (e.g., `feat: implement token bucket rate limit strategy`).
- **`fix:`** A bug fix (e.g., `fix: correct retry-after calculation for sliding window`).
- **`docs:`** Documentation changes only (e.g., `docs: add SRS document and ER diagram`).
- **`test:`** Adding or correcting unit/integration tests (e.g., `test: add concurrency test for token bucket`).
- **`refactor:`** Code changes that neither fix a bug nor add a feature (e.g., `refactor: extract key hashing to utility class`).
- **`chore:`** Build process, configuration, or dependency updates (e.g., `chore: update maven compiler release version`).

---

## 4. Pull Request & Review Lifecycle
1. **Branch Synchronization:** Prior to opening a PR, rebase your feature branch on the latest `main`:
   ```bash
   git fetch origin
   git rebase origin/main
   ```
2. **Local Pre-Flight Checks:** Run full local test and build verifications:
   ```bash
   # Backend check
   cd backend && mvn clean test
   
   # Frontend check
   cd ../frontend && npm run build
   ```
3. **Open Pull Request:** Complete the standard [Pull Request Template](../../.github/PULL_REQUEST_TEMPLATE.md).
4. **Peer Review:** At least one team member must approve the PR before merging.
5. **Merge Strategy:** Prefer **Squash and Merge** or **Rebase and Merge** to maintain a linear, readable git history on `main`.
