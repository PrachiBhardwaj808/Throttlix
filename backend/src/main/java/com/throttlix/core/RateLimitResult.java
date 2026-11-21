package com.throttlix.core;

public class RateLimitResult {

    private final boolean allowed;
    private final double remainingCapacity;
    private final Long retryAfterMillis;
    private final String message;

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

    public static RateLimitResult allow(double remainingCapacity, String message) {
        return new RateLimitResult(true, remainingCapacity, null, message);
    }

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
