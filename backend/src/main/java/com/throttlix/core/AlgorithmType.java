package com.throttlix.core;

/**
 * Enumeration of rate-limiting algorithms supported by Throttlix.
 *
 * <p><b>Why use an enum instead of arbitrary Strings?</b>
 * <ul>
 *   <li><b>Compile-Time Safety:</b> Prevents spelling mistakes (e.g., passing "token_bucket"
 *       or "TokenBucket" instead of a recognized value).</li>
 *   <li><b>Restricted Domain:</b> Guarantees that only valid, supported algorithms can ever be assigned.</li>
 *   <li><b>Refactoring & Switch Safety:</b> The Java compiler can verify that all algorithm cases
 *       are handled in switch statements.</li>
 * </ul>
 */
public enum AlgorithmType {

    /**
     * Token Bucket Algorithm:
     * Maintains a bucket of tokens up to a maximum capacity.
     * Tokens refill at a steady rate over time.
     * Each request consumes one token; allows short bursts up to bucket capacity.
     * Assigned to Prachi.
     */
    TOKEN_BUCKET,

    /**
     * Sliding Window Algorithm:
     * Tracks the exact timestamps of requests over a moving time window.
     * Prevents boundary-burst exploits by evaluating traffic over a rolling duration.
     * Assigned to Akshit.
     */
    SLIDING_WINDOW
}
