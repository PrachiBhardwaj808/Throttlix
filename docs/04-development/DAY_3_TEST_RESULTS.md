# Day 3 Test Results

## Scope

Day 3 validates the Token Bucket and Sliding Window algorithms with the existing JUnit 5 test classes:

- `backend/src/test/java/com/throttlix/algorithm/tokenbucket/TokenBucketRateLimiterTest.java`
- `backend/src/test/java/com/throttlix/algorithm/slidingwindow/SlidingWindowTest.java`

## Run

Command, run from `backend/`:

```bash
mvn clean test
```

## Result

Verified on 2026-11-22 using Maven 3.10.1 and OpenJDK 25.0.3:

- Tests run: 16
- Passed: 16
- Failures: 0
- Errors: 0
- Skipped: 0
- Build: `BUILD SUCCESS`

The Token Bucket suite ran 9 tests. The Sliding Window suite ran 7 tests. Coverage includes allowed and rejected requests, remaining capacity, refill and expiry behavior, retry timing, per-client state, disabled or mismatched policies, backwards timestamps, and concurrent requests.

## Status

The existing Day 3 algorithm tests pass on the current `main`. This records test execution only; it does not claim that later milestones such as the traffic simulator, HTTP gateway, or database-backed policy flow are complete.
