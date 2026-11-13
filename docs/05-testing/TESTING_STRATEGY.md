# Testing Strategy — Throttlix

## 1. Overview & Test Objectives
The **Throttlix Testing Strategy** establishes a multi-layered verification framework to guarantee mathematical accuracy of rate-limiting algorithms, strict thread-safety under heavy concurrency, resilient failure handling, and end-to-end API correctness.

---

## 2. Test Levels & Pyramid

```text
               / \
              /   \
             / E2E \       API Integration (Postman) & Load (k6 / JMeter)
            /-------\
           / Service \     Isolated Service & DAO Tests (Mockito)
          /-----------\
         / Unit & Conc \   Algorithm & Concurrency Tests (JUnit 5)
        /---------------\
```

| Test Level | Scope & Target Components | Primary Tools | Automation Level |
| :--- | :--- | :--- | :--- |
| **Unit Testing** | Rate-limiting algorithms (`TokenBucketStrategy`, `SlidingWindowStrategy`), hashing utilities, and parameter validators. | JUnit 5 Jupiter | 100% Automated (`mvn test`) |
| **Concurrency Testing** | Multi-threaded race condition tests evaluating parallel token consumption against a shared counter. | JUnit 5, `ExecutorService`, `CountDownLatch` | 100% Automated (`mvn test`) |
| **Service Integration** | Database repository queries (`ApiKeyDAO`, `PolicyDAO`, `RequestLogDAO`) and transaction handling. | JUnit 5, Mockito, Local PostgreSQL | Automated (`mvn test`) |
| **API End-to-End** | Full HTTP request/response validation across servlets, headers, and status codes (`200`, `400`, `401`, `429`). | Postman Collection, Newman CLI | Automated / Manual Script |
| **Performance & Load** | High-concurrency traffic burst generation to observe gateway latency and 429 threshold precision. | JMeter or k6 | Automated Benchmark |
| **UI Verification** | Dashboard metric accuracy, chart rendering, and API integration. | Browser / Manual Verification | Manual Test Script |

---

## 3. Dedicated Algorithm & Concurrency Test Methodology
Because Throttlix is a traffic-control layer, concurrency defects (such as double-spending a final remaining token) are critical bugs.

### Concurrency Stress Test Design:
1. Initialize a `TokenBucketStrategy` with capacity $C = 5$ and refill rate $R = 0$ tokens/second.
2. Spawn $N = 50$ concurrent worker threads using an `ExecutorService`.
3. Use a `CountDownLatch` to release all 50 threads simultaneously against the rate limiter.
4. Collect all `RateLimitDecision` results.
5. **Assertion:** Exactly 5 decisions must be `ALLOW` (`allowed = true`), and exactly 45 decisions must be `REJECT` (`allowed = false`). Under no circumstances may the allowed count exceed 5.

---

## 4. Test Data Management & Security
- Tests must use dedicated, isolated test API keys and mock clients.
- Never use production credentials or real-world secrets in test suites.
- Repository tests must clean up created test records in teardown methods (`@AfterEach`) or execute within rollbacked transactions.
