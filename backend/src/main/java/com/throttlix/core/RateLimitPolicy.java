package com.throttlix.core;

/**
 * Immutable configuration parameters for rate-limiting rules.
 * Defines request capacity, window intervals, and algorithm settings.
 *
 * @param policyName           descriptive name of the policy (e.g. "Standard-Tier")
 * @param maxRequests          maximum permitted requests within the window
 * @param windowDurationMillis duration of the rate-limiting window in milliseconds
 */
public record RateLimitPolicy(
        String policyName,
        int maxRequests,
        long windowDurationMillis
) {
    public RateLimitPolicy {
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be strictly positive");
        }
        if (windowDurationMillis <= 0) {
            throw new IllegalArgumentException("windowDurationMillis must be strictly positive");
        }
    }
}
