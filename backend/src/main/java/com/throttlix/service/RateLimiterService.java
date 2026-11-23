package com.throttlix.service;

import com.throttlix.algorithm.slidingwindow.SlidingWindow;
import com.throttlix.algorithm.tokenbucket.TokenBucketRateLimiter;
import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.core.RateLimitResult;
import com.throttlix.core.RateLimiter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RateLimiterService {

    private final RateLimiter tokenBucketLimiter;
    private final RateLimiter slidingWindowLimiter;
    private final ConcurrentMap<String, RateLimitPolicy> clientPolicies = new ConcurrentHashMap<>();

    public RateLimiterService() {
        this(new TokenBucketRateLimiter(), new SlidingWindow());
    }

    public RateLimiterService(RateLimiter tokenBucketLimiter, RateLimiter slidingWindowLimiter) {
        this.tokenBucketLimiter = tokenBucketLimiter;
        this.slidingWindowLimiter = slidingWindowLimiter;
        initializeDefaultPolicies();
    }

    private void initializeDefaultPolicies() {
        clientPolicies.put("client-analytics", RateLimitPolicy.createTokenBucketPolicy(
                "pol-analytics", "client-analytics", 10, 5, 1000
        ));
        clientPolicies.put("client-mobile", RateLimitPolicy.createSlidingWindowPolicy(
                "pol-mobile", "client-mobile", 5, 10000
        ));
    }

    public RateLimitPolicy getPolicy(String clientId) {
        if (clientId == null) {
            return null;
        }
        return clientPolicies.computeIfAbsent(clientId, id ->
                RateLimitPolicy.createTokenBucketPolicy(
                        "pol-default-" + id, id, 10, 5, 1000
                )
        );
    }

    public void registerPolicy(RateLimitPolicy policy) {
        if (policy != null && policy.getClientId() != null) {
            clientPolicies.put(policy.getClientId(), policy);
        }
    }

    public RateLimitResult evaluate(String clientId, long currentTimeMillis) {
        RateLimitPolicy policy = getPolicy(clientId);
        if (policy == null) {
            return RateLimitResult.reject(0.0, null, "No rate limit policy found");
        }

        if (policy.getAlgorithmType() == AlgorithmType.SLIDING_WINDOW) {
            return slidingWindowLimiter.allowRequest(policy, currentTimeMillis);
        }

        return tokenBucketLimiter.allowRequest(policy, currentTimeMillis);
    }
}
