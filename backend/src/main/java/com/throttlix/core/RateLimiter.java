package com.throttlix.core;

public interface RateLimiter {

    RateLimitResult allowRequest(
            RateLimitPolicy policy,
            long currentTimeMillis);
}
