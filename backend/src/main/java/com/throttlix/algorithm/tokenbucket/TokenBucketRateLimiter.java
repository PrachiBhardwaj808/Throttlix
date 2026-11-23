package com.throttlix.algorithm.tokenbucket;

import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.core.RateLimitResult;
import com.throttlix.core.RateLimiter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TokenBucketRateLimiter implements RateLimiter {

    private final ConcurrentMap<String, TokenBucketState> clientStates = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult allowRequest(RateLimitPolicy policy, long currentTimeMillis) {
        if (policy == null) {
            return RateLimitResult.reject(0.0, null, "Rate limit policy is required");
        }

        if (policy.getAlgorithmType() != AlgorithmType.TOKEN_BUCKET) {
            return RateLimitResult.reject(0.0, null, "Policy must use the Token Bucket algorithm");
        }

        if (!policy.isEnabled()) {
            return RateLimitResult.allow(policy.getLimit(), "Rate limit policy is disabled");
        }

        String clientId = policy.getClientId();
        TokenBucketState state = clientStates.get(clientId);

        if (state == null) {
            TokenBucketState newState = new TokenBucketState(
                    policy.getLimit(),
                    policy.getRefillTokens(),
                    policy.getRefillDurationMillis(),
                    currentTimeMillis
            );
            TokenBucketState existing = clientStates.putIfAbsent(clientId, newState);
            state = (existing != null) ? existing : newState;
        }

        synchronized (state) {
            boolean consumed = state.tryConsume(currentTimeMillis);
            double remainingTokens = state.getAvailableTokens();

            if (consumed) {
                return RateLimitResult.allow(remainingTokens, "Request allowed by Token Bucket");
            }

            long retryAfterMillis = state.calculateRetryAfterMillis(currentTimeMillis);
            return RateLimitResult.reject(remainingTokens, retryAfterMillis, "Token Bucket rate limit exceeded");
        }
    }
}
