
package com.throttlix.core;

import java.util.Objects;

public class RateLimitPolicy {

    private String policyId;
    private String clientId;
    private AlgorithmType algorithmType;
    private int limit;
    private long windowDurationMillis;
    private int refillTokens;
    private long refillDurationMillis;
    private boolean enabled;

    public RateLimitPolicy(
            String policyId,
            String clientId,
            AlgorithmType algorithmType,
            int limit,
            long windowDurationMillis,
            int refillTokens,
            long refillDurationMillis,
            boolean enabled) {

        if (policyId == null || policyId.trim().isEmpty()) {
            throw new IllegalArgumentException("Policy ID cannot be empty");
        }

        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Client ID cannot be empty");
        }

        this.algorithmType = Objects.requireNonNull(algorithmType);

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }

        if (algorithmType == AlgorithmType.SLIDING_WINDOW) {

            if (windowDurationMillis <= 0) {
                throw new IllegalArgumentException("Window duration must be greater than 0");
            }

            this.windowDurationMillis = windowDurationMillis;
            this.refillTokens = 0;
            this.refillDurationMillis = 0;

        } else if (algorithmType == AlgorithmType.TOKEN_BUCKET) {

            if (refillTokens <= 0) {
                throw new IllegalArgumentException("Refill tokens must be greater than 0");
            }

            if (refillDurationMillis <= 0) {
                throw new IllegalArgumentException("Refill duration must be greater than 0");
            }

            this.windowDurationMillis = 0;
            this.refillTokens = refillTokens;
            this.refillDurationMillis = refillDurationMillis;
        }

        this.policyId = policyId.trim();
        this.clientId = clientId.trim();
        this.limit = limit;
        this.enabled = enabled;
    }

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
                0,
                refillTokens,
                refillDurationMillis,
                true);
    }

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
                0,
                true);
    }

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
        return "RateLimitPolicy{" +
                "policyId='" + policyId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", algorithmType=" + algorithmType +
                ", limit=" + limit +
                ", enabled=" + enabled +
                '}';
    }
}
