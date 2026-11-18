package com.throttlix.core;

/**
 * Result model representing the outcome of a rate-limiting evaluation in Throttlix.
 *
 * <p>This object is returned by {@link RateLimiter#allowRequest(RateLimitPolicy, long)}
 * and communicates:
 * <ul>
 *   <li>Whether the request was allowed or blocked.</li>
 *   <li>The remaining capacity (tokens or request slots).</li>
 *   <li>The recommended delay before retrying (if rejected and known).</li>
 *   <li>A descriptive status message.</li>
 * </ul>
 *
 * <h3>Why is {@code remainingCapacity} a {@code double}?</h3>
 * <ul>
 *   <li>In <b>Token Bucket</b>, tokens are replenished continuously over elapsed time, which can
 *       result in fractional amounts (e.g., 3.5 tokens). A {@code double} handles fractional values
 *       accurately.</li>
 *   <li>In <b>Sliding Window</b>, it represents whole remaining request slots (e.g., 4.0 slots remaining).</li>
 * </ul>
 *
 * <h3>Why is {@code retryAfterMillis} a {@code Long} (wrapper object)?</h3>
 * <ul>
 *   <li>When a request is <b>allowed</b>, retry timing is not applicable. Using {@code Long} allows
 *       storing {@code null} instead of an ambiguous sentinel number like 0 or -1.</li>
 *   <li>If retry timing cannot be calculated reliably, {@code null} is used rather than guessing.</li>
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
     * @param allowed           true if the request is permitted; false if rejected
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
