package com.throttlix.core;

/**
 * Encapsulates the outcome of a rate-limiting evaluation.
 *
 * @param allowed           true if the request is permitted, false if rejected
 * @param remainingTokens   number of tokens or allowed requests remaining
 * @param retryAfterSeconds duration in seconds to wait before retrying (0 if allowed)
 * @param message           human-readable status message
 */
public record RateLimitResult(
        boolean allowed,
        long remainingTokens,
        long retryAfterSeconds,
        String message
) {
    public static RateLimitResult allow(long remainingTokens) {
        return new RateLimitResult(true, remainingTokens, 0, "Request allowed");
    }

    public static RateLimitResult reject(long retryAfterSeconds) {
        return new RateLimitResult(false, 0, retryAfterSeconds, "Rate limit exceeded. Try again later.");
    }
}
