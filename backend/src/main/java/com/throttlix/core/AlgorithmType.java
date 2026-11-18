package com.throttlix.core;

/**
 * Enumeration of rate-limiting algorithms supported by Throttlix.
 *
 * <p>Why use an enum instead of arbitrary Strings?
 * <ul>
 *   <li><b>Type Safety:</b> Prevents typos like "token_bucket" vs "TokenBucket" at compile time.</li>
 *   <li><b>Limited Values:</b> Ensures only valid, supported algorithms can be selected.</li>
 *   <li><b>Switch Statement Safety:</b> Java can check that all algorithm cases are handled.</li>
 * </ul>
 */
public enum AlgorithmType {

    /**
     * Token Bucket Algorithm:
     * Maintains a bucket of tokens up to a maximum capacity.
     * Each request consumes a token. Tokens are added back at a steady refill rate.
     * Suitable for handling short bursts of traffic while enforcing an average rate.
     */
    TOKEN_BUCKET,

    /**
     * Sliding Window Algorithm:
     * Tracks the exact timestamps of requests within a moving time window.
     * Rejects requests if the number of requests in the window exceeds the limit.
     * Provides strict, boundary-safe rate limiting without burst edge exploits.
     */
    SLIDING_WINDOW
}
