package com.throttlix.core;

import java.util.Objects;

/**
 * Configuration model for rate-limiting rules in Throttlix.
 *
 * <p>This is a shared model used by both:
 * <ul>
 *   <li><b>Token Bucket</b> (implemented by Prachi)</li>
 *   <li><b>Sliding Window</b> (implemented by Akshit)</li>
 * </ul>
 *
 * <h3>Meaning of {@code limit} in Both Algorithms:</h3>
 * <ul>
 *   <li><b>Token Bucket:</b> {@code limit} represents the <i>maximum bucket capacity</i>
 *       (the maximum number of tokens the bucket can hold, allowing bursts up to this number).</li>
 *   <li><b>Sliding Window:</b> {@code limit} represents the <i>maximum allowed requests</i>
 *       within the moving time window defined by {@code windowDurationMillis}.</li>
 * </ul>
 *
 * <h3>Algorithm-Specific Fields:</h3>
 * <ul>
 *   <li>For <b>Token Bucket</b>: {@code refillTokens} and {@code refillDurationMillis} define how
 *       quickly tokens replenish. {@code windowDurationMillis} is not used (set to 0).</li>
 *   <li>For <b>Sliding Window</b>: {@code windowDurationMillis} defines the window size in milliseconds.
 *       {@code refillTokens} and {@code refillDurationMillis} are not used (set to 0).</li>
 * </ul>
 */
public class RateLimitPolicy {

    // Identity and algorithm
    private final String policyId;
    private final String clientId;
    private final AlgorithmType algorithmType;

    // Core limit parameter
    private final int limit;

    // Sliding Window specific (unused for Token Bucket)
    private final long windowDurationMillis;

    // Token Bucket specific (unused for Sliding Window)
    private final int refillTokens;
    private final long refillDurationMillis;

    // Status flag
    private final boolean enabled;

    /**
     * Primary constructor with strict validation.
     *
     * @param policyId             unique identifier for this policy (e.g., "policy-standard-tier")
     * @param clientId             client or API key identifier associated with this policy (e.g., "client-acme")
     * @param algorithmType        the algorithm to use (TOKEN_BUCKET or SLIDING_WINDOW)
     * @param limit                burst capacity (Token Bucket) or max requests per window (Sliding Window)
     * @param windowDurationMillis window duration in milliseconds (required for Sliding Window, 0 for Token Bucket)
     * @param refillTokens         tokens added per refill cycle (required for Token Bucket, 0 for Sliding Window)
     * @param refillDurationMillis duration of each refill cycle in milliseconds (required for Token Bucket, 0 for Sliding Window)
     * @param enabled              whether this policy is currently active
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

        // 4. Algorithm-specific validation
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
     * Convenience factory method to create a Token Bucket policy.
     *
     * @param policyId             unique identifier for this policy
     * @param clientId             client identifier
     * @param capacity             maximum bucket token capacity
     * @param refillTokens         tokens refilled per period
     * @param refillDurationMillis refill period duration in milliseconds
     * @return a configured RateLimitPolicy for Token Bucket
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
     * Convenience factory method to create a Sliding Window policy.
     *
     * @param policyId             unique identifier for this policy
     * @param clientId             client identifier
     * @param maxRequests          maximum allowed requests within the window
     * @param windowDurationMillis length of the sliding window in milliseconds
     * @return a configured RateLimitPolicy for Sliding Window
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
