package com.throttlix.core;

/**
 * Core contract for rate-limiting algorithms in Throttlix.
 * Both Token Bucket and Sliding Window implementations adhere to this interface.
 */
public interface RateLimiter {

    /**
     * Evaluates whether an incoming request from a client is allowed under the configured policy.
     *
     * @param clientId the unique identifier of the client or API key
     * @return the evaluation result containing the allow/block decision and rate telemetry
     */
    RateLimitResult allowRequest(String clientId);
}
