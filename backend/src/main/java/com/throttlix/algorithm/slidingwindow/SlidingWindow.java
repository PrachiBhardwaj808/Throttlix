package com.throttlix.algorithm.slidingwindow;

import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.core.RateLimitResult;
import com.throttlix.core.RateLimiter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SlidingWindow implements RateLimiter {

    private final ConcurrentMap<String, SlidingWindowState> clientStates = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult allowRequest(RateLimitPolicy policy, long currentTimeMillis) {
        if (policy == null) {
            return RateLimitResult.reject(0.0, null, "Rate limit policy is required");
        }

        if (policy.getAlgorithmType() != AlgorithmType.SLIDING_WINDOW) {
            return RateLimitResult.reject(0.0, null, "Policy must use the Sliding Window algorithm");
        }

        if (!policy.isEnabled()) {
            return RateLimitResult.reject(0.0, null, "Rate limit policy is disabled");
        }

        String clientId = policy.getClientId();
        SlidingWindowState state = clientStates.get(clientId);

        if (state == null) {
            SlidingWindowState newState = new SlidingWindowState();
            SlidingWindowState existing = clientStates.putIfAbsent(clientId, newState);
            state = (existing != null) ? existing : newState;
        }

        synchronized (state) {
            Long latestTimestamp = state.getLatestRequestTimestamp();
            long safeCurrentTime = (latestTimestamp == null)
                    ? currentTimeMillis
                    : Math.max(currentTimeMillis, latestTimestamp);

            long windowDurationMillis = policy.getWindowDurationMillis();
            state.removeExpiredTimestamps(safeCurrentTime, windowDurationMillis);

            int currentRequests = state.getRequestCount();
            int maxRequests = policy.getLimit();

            if (currentRequests < maxRequests) {
                state.addRequestTimestamp(safeCurrentTime);
                int remainingRequests = maxRequests - currentRequests - 1;
                return RateLimitResult.allow(remainingRequests, "Request allowed by Sliding Window");
            }

            Long oldestTimestamp = state.getOldestRequestTimestamp();
            long retryAfterMillis = (oldestTimestamp == null)
                    ? windowDurationMillis
                    : Math.max(0L, oldestTimestamp + windowDurationMillis - safeCurrentTime);

            return RateLimitResult.reject(0.0, retryAfterMillis, "Sliding Window request limit exceeded");
        }
    }
}
