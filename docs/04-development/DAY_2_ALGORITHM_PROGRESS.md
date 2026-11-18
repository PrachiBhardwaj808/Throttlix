# Day 2 Algorithm Progress — Akshit and Prachi

This page records the current Day 2 implementation, explains the Sliding Window code, and tracks the remaining work for both developers. It reflects the repository state after the Sliding Window implementation was merged to `main`.

## Shared contract

Both algorithms use the shared classes in `backend/src/main/java/com/throttlix/core/`:

- `RateLimiter` defines `allowRequest(RateLimitPolicy policy, long currentTimeMillis)`.
- `RateLimitPolicy` identifies the client and algorithm, sets the limit, and supplies the algorithm's time settings.
- `RateLimitResult` reports whether the request was allowed, remaining capacity, and retry delay in milliseconds.

The explicit timestamp lets callers and tests control time without waiting. A Sliding Window policy is created with `RateLimitPolicy.createSlidingWindowPolicy(policyId, clientId, maxRequests, windowDurationMillis)`.

## Akshit's Sliding Window implementation

Files:

- [`SlidingWindow.java`](../../backend/src/main/java/com/throttlix/algorithm/slidingwindow/SlidingWindow.java)
- [`SlidingWindowState.java`](../../backend/src/main/java/com/throttlix/algorithm/slidingwindow/SlidingWindowState.java)

`SlidingWindow` keeps a `ConcurrentHashMap` of client IDs to independent states. For each request it checks that the policy is present, enabled, and configured for Sliding Window. It then locks that client's state, removes timestamps outside the active window, and compares the remaining count with the configured limit. Allowed requests are timestamped; rejected requests get a retry delay based on the oldest active timestamp.

`SlidingWindowState` stores accepted request timestamps in an `ArrayDeque`. Since accepted timestamps are appended in order, the oldest request is at the front and the newest is at the back. The active interval is `(current time - window duration, current time]`; timestamps at or before its start are expired. The algorithm clamps a backwards-moving supplied clock to the latest accepted timestamp for that client, preserving timestamp order.

The per-client lock makes pruning, counting, deciding, and recording one atomic operation for that client. Different clients keep separate queues.

## Work and ownership tracker

| Area | Akshit | Prachi | Status |
| :--- | :--- | :--- | :--- |
| Shared rate-limit contract | Review timestamp and result behavior; use the agreed models | Own shared policy, result, and interface models | Shared contract is on `main` |
| Token Bucket (FR-03) | Review compatibility during integration | Implement bucket state, refill, consumption, and retry behavior | Prachi's implementation and validation remain to be confirmed |
| Sliding Window (FR-04) | Implement per-client timestamp state and rolling-window decisions | Review compatibility with the shared models | Implementation merged to `main`; validation remains |
| Algorithm tests | Add controlled-time boundary, expiry, limit, retry, and per-client tests | Add Token Bucket refill, capacity, and client-isolation tests | Pending for both |
| Concurrency and simulation | Validate that parallel requests cannot exceed the limit; build traffic simulation and compare algorithms | Support integration and validate Token Bucket behavior | Pending |
| Common integration and demo | Exercise both algorithms through the shared interface and help document results | Integrate both algorithms and coordinate the demo | Pending until both implementations and tests are ready |

## Akshit's remaining work

1. Add controlled-time tests for allowed requests, limit reached, expired timestamps, exact window boundary, retry delay, separate clients, disabled/wrong policies, and backwards time.
2. Add a concurrent-request test proving that no more than the configured limit is accepted for one client.
3. Build the planned traffic simulator and compare Sliding Window with Token Bucket using the same request patterns.
4. Coordinate with Prachi to verify both algorithms use the shared models and compile together; record any integration issues and demo results.
5. Update this page as each item is completed and link the corresponding merged PRs.

## Validation status

The Sliding Window source and shared core classes were compiled together with `javac` in the development environment. Maven validation was not run because `mvn` was unavailable there. Automated algorithm tests have not yet been added.
