package com.throttlix.core;

/**
 * Common interface for all rate-limiting algorithms in Throttlix.
 *
 * <p>This shared interface is implemented by both:
 * <ul>
 *   <li><b>Token Bucket</b> (implemented by Prachi in {@code com.throttlix.algorithm.tokenbucket})</li>
 *   <li><b>Sliding Window</b> (implemented by Akshit in {@code com.throttlix.algorithm.slidingwindow})</li>
 * </ul>
 *
 * <p>By agreeing on this single interface, the rest of the application can evaluate
 * requests uniformly without caring which underlying algorithm is executing.
 */
public interface RateLimiter {

    /**
     * Evaluates whether an incoming request from a client is allowed under the given policy
     * at a specific point in time.
     *
     * <p>Why accept {@code currentTimeMillis}?
     * Passing time explicitly allows Akshit and Prachi to test time-based behavior (refilling tokens
     * or expiring windows) deterministically in tests and simulations without calling {@code Thread.sleep()}.
     *
     * @param policy            the rate limit policy configuration containing limits and algorithm rules
     * @param currentTimeMillis the current timestamp in epoch milliseconds (System.currentTimeMillis())
     * @return a {@link RateLimitResult} containing the decision (allowed or rejected), remaining capacity,
     *         and retry delay
     */
    RateLimitResult allowRequest(
            RateLimitPolicy policy,
            long currentTimeMillis
    );
}
