package com.throttlix.core;

/**
 * Result model representing the outcome of a rate-limiting check in Throttlix.
 *
 * <p>This object is returned by {@link RateLimiter#allowRequest(RateLimitPolicy, long)}
 * and provides callers with:
 * <ul>
 *   <li>Whether the request was allowed or blocked (HTTP 200 vs. HTTP 429).</li>
 *   <li>The remaining capacity available to the client.</li>
 *   <li>The delay to wait before retrying (if rejected and known).</li>
 *   <li>A clear, human-readable status message.</li>
 * </ul>
 *
 * <h3>Why is {@code remainingCapacity} a {@code double}?</h3>
 * <ul>
 *   <li>In <b>Token Bucket</b>, tokens are replenished continuously based on elapsed time, which
 *       often yields fractional token counts (e.g., 3.5 tokens). Using {@code double} prevents
 *       premature truncation and precision loss.</li>
 *   <li>In <b>Sliding Window</b>, it represents whole remaining request slots (e.g., 4.0 slots remaining).</li>
 * </ul>
 *
 * <h3>Why is {@code retryAfterMillis} a {@code Long} wrapper object?</h3>
 * <ul>
 *   <li>When a request is <b>allowed</b>, a retry delay does not apply. The object wrapper {@code Long}
 *       allows us to return {@code null} to clearly represent "not applicable".</li>
 *   <li>If primitive {@code long} were used, we would have to use an ambiguous number like 0 or -1.
 *       Returning 0 ms might mislead a client into retrying immediately!</li>
 *   <li>We never guess retry timing. If the algorithm cannot calculate an exact retry time,
 *       {@code null} is returned.</li>
 * </ul>
 */
public class RateLimitResult {

    private final boolean allowed;
    private final double remainingCapacity;
    private final Long retryAfterMillis;
    private final String message;

    /**
     * Primary constructor.
     *
     * @param allowed           true if request is permitted; false if rate limit is exceeded
     * @param remainingCapacity available tokens (Token Bucket) or remaining slots (Sliding Window)
     * @param retryAfterMillis  delay in milliseconds before the client may retry (null if allowed or unknown)
     * @param message           explanatory message describing the outcome
     */
    public RateLimitResult(
            boolean allowed,
            double remainingCapacity,
            Long retryAfterMillis,
            String message) {
        this.allowed = allowed;
        this.remainingCapacity = remainingCapacity;
        this.retryAfterMillis = retryAfterMillis;
        this.message = (message != null) ? message : (allowed ? "Request allowed" : "Rate limit exceeded");
    }

    /**
     * Convenience factory method for an allowed request.
     *
     * @param remainingCapacity remaining tokens or request allowance
     * @param message           custom status message
     * @return a successful RateLimitResult with null retry delay
     */
    public static RateLimitResult allow(double remainingCapacity, String message) {
        return new RateLimitResult(true, remainingCapacity, null, message);
    }

    /**
     * Convenience factory method for a rejected (rate-limited) request.
     *
     * @param remainingCapacity remaining capacity (typically 0.0)
     * @param retryAfterMillis  recommended wait time in milliseconds, or null if uncalculated
     * @param message           rejection explanation
     * @return a rejected RateLimitResult
     */
    public static RateLimitResult reject(double remainingCapacity, Long retryAfterMillis, String message) {
        return new RateLimitResult(false, remainingCapacity, retryAfterMillis, message);
    }

    // Getters

    public boolean isAllowed() {
        return allowed;
    }

    public double getRemainingCapacity() {
        return remainingCapacity;
    }

    public Long getRetryAfterMillis() {
        return retryAfterMillis;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "RateLimitResult["
                + "allowed=" + allowed
                + ", remainingCapacity=" + remainingCapacity
                + ", retryAfterMillis=" + (retryAfterMillis != null ? retryAfterMillis + "ms" : "N/A")
                + ", message='" + message + '\''
                + ']';
    }
}
