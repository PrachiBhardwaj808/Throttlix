package com.throttlix.core;

import java.util.Objects;

/**
 * Configuration model defining the rate-limiting rules for a client in Throttlix.
 *
 * <p>This shared model is used by both:
 * <ul>
 *   <li><b>Token Bucket</b> (implemented by Prachi)</li>
 *   <li><b>Sliding Window</b> (implemented by Akshit)</li>
 * </ul>
 *
 * <h3>Why Token Bucket and Sliding Window Require Different Configurations:</h3>
 * <ul>
 *   <li><b>Token Bucket</b> works with a bucket of tokens that replenishes over time.
 *       It needs to know:
 *       <ol>
 *         <li>How many tokens the bucket can hold at most (capacity).</li>
 *         <li>How many tokens to add back (refill tokens).</li>
 *         <li>How often to add those tokens (refill interval in milliseconds).</li>
 *       </ol>
 *       It does <i>not</i> use a fixed observation window, so {@code windowDurationMillis} is unused (0).
 *   </li>
 *   <li><b>Sliding Window</b> works by counting timestamps within a moving time window.
 *       It needs to know:
 *       <ol>
 *         <li>The maximum number of requests allowed in that window.</li>
 *         <li>The length of the moving window in milliseconds (e.g., 60,000 ms for 1 minute).</li>
 *       </ol>
 *       It does <i>not</i> have tokens or a refill cycle, so {@code refillTokens} and {@code refillDurationMillis} are unused (0).
 *   </li>
 * </ul>
 *
 * <h3>Meaning of {@code limit} in Both Algorithms:</h3>
 * <ul>
 *   <li><b>Token Bucket:</b> {@code limit} represents the <i>maximum bucket capacity</i>
 *       (maximum tokens allowed in the bucket, permitting bursts up to this limit).</li>
 *   <li><b>Sliding Window:</b> {@code limit} represents the <i>maximum allowed requests</i>
 *       within the moving time window defined by {@code windowDurationMillis}.</li>
 * </ul>
 */
public class RateLimitPolicy {

    // Identity and algorithm selection
    private final String policyId;
    private final String clientId;
    private final AlgorithmType algorithmType;

    // Core limit parameter (dual meaning depending on algorithm)
    private final int limit;

    // Sliding Window specific (unused for Token Bucket, set to 0)
    private final long windowDurationMillis;

    // Token Bucket specific (unused for Sliding Window, set to 0)
    private final int refillTokens;
    private final long refillDurationMillis;

    // Operational switch
    private final boolean enabled;

    /**
     * Primary constructor with complete parameter validation.
     *
     * @param policyId             unique identifier for this policy (e.g., "policy-standard-tier")
     * @param clientId             client or API key identifier (e.g., "client-acme-corp")
     * @param algorithmType        the rate-limiting algorithm (TOKEN_BUCKET or SLIDING_WINDOW)
     * @param limit                burst capacity (Token Bucket) or max requests per window (Sliding Window)
     * @param windowDurationMillis window length in milliseconds (required for Sliding Window; 0 for Token Bucket)
     * @param refillTokens         tokens added per refill cycle (required for Token Bucket; 0 for Sliding Window)
     * @param refillDurationMillis duration of refill cycle in milliseconds (required for Token Bucket; 0 for Sliding Window)
     * @param enabled              true if policy is actively enforced; false if disabled
     */
    public RateLimitPolicy(
            String policyId,
            String clientId,
            AlgorithmType algorithmType,
            int limit,
            long windowDurationMillis,
            int refillTokens,
            long refillDurationMillis,
            boolean enabled) {

        // 1. Validate required text identifiers
        if (policyId == null || policyId.trim().isEmpty()) {
            throw new IllegalArgumentException("policyId cannot be null or empty");
        }
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("clientId cannot be null or empty");
        }

        // 2. Validate algorithm type
        this.algorithmType = Objects.requireNonNull(algorithmType, "algorithmType cannot be null");

        // 3. Validate limit (must be strictly positive for both algorithms)
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0");
        }

        // 4. Algorithm-specific validation rules
        if (algorithmType == AlgorithmType.SLIDING_WINDOW) {
            if (windowDurationMillis <= 0) {
                throw new IllegalArgumentException("windowDurationMillis must be greater than 0 for Sliding Window");
            }
            this.windowDurationMillis = windowDurationMillis;
            this.refillTokens = 0;
            this.refillDurationMillis = 0L;
        } else if (algorithmType == AlgorithmType.TOKEN_BUCKET) {
            if (refillTokens <= 0) {
                throw new IllegalArgumentException("refillTokens must be greater than 0 for Token Bucket");
            }
            if (refillDurationMillis <= 0) {
                throw new IllegalArgumentException("refillDurationMillis must be greater than 0 for Token Bucket");
            }
            this.windowDurationMillis = 0L;
            this.refillTokens = refillTokens;
            this.refillDurationMillis = refillDurationMillis;
        } else {
            throw new IllegalArgumentException("Unsupported algorithm type: " + algorithmType);
        }

        this.policyId = policyId.trim();
        this.clientId = clientId.trim();
        this.limit = limit;
        this.enabled = enabled;
    }

    /**
     * Convenience factory method for constructing a Token Bucket policy.
     *
     * @param policyId             unique identifier for this policy
     * @param clientId             client identifier
     * @param capacity             maximum bucket capacity (limit)
     * @param refillTokens         tokens added per refill interval
     * @param refillDurationMillis duration of the refill interval in milliseconds
     * @return a configured, validated Token Bucket policy
     */
    public static RateLimitPolicy createTokenBucketPolicy(
            String policyId,
            String clientId,
            int capacity,
            int refillTokens,
            long refillDurationMillis) {
        return new RateLimitPolicy(
                policyId,
                clientId,
                AlgorithmType.TOKEN_BUCKET,
                capacity,
                0L,
                refillTokens,
                refillDurationMillis,
                true
        );
    }

    /**
     * Convenience factory method for constructing a Sliding Window policy.
     *
     * @param policyId             unique identifier for this policy
     * @param clientId             client identifier
     * @param maxRequests          maximum allowed requests within the window (limit)
     * @param windowDurationMillis window duration in milliseconds
     * @return a configured, validated Sliding Window policy
     */
    public static RateLimitPolicy createSlidingWindowPolicy(
            String policyId,
            String clientId,
            int maxRequests,
            long windowDurationMillis) {
        return new RateLimitPolicy(
                policyId,
                clientId,
                AlgorithmType.SLIDING_WINDOW,
                maxRequests,
                windowDurationMillis,
                0,
                0L,
                true
        );
    }

    // Getters

    public String getPolicyId() {
        return policyId;
    }

    public String getClientId() {
        return clientId;
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public int getLimit() {
        return limit;
    }

    public long getWindowDurationMillis() {
        return windowDurationMillis;
    }

    public int getRefillTokens() {
        return refillTokens;
    }

    public long getRefillDurationMillis() {
        return refillDurationMillis;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        if (algorithmType == AlgorithmType.TOKEN_BUCKET) {
            return "RateLimitPolicy[TOKEN_BUCKET, policyId=" + policyId
                    + ", clientId=" + clientId
                    + ", capacity=" + limit
                    + ", refill=" + refillTokens + " tokens per " + refillDurationMillis + "ms"
                    + ", enabled=" + enabled + "]";
        } else {
            return "RateLimitPolicy[SLIDING_WINDOW, policyId=" + policyId
                    + ", clientId=" + clientId
                    + ", limit=" + limit + " requests per " + windowDurationMillis + "ms"
                    + ", enabled=" + enabled + "]";
        }
    }
}
