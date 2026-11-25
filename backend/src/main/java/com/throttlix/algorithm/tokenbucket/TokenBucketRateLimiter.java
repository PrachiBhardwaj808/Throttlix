package com.throttlix.algorithm.tokenbucket;

import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.core.RateLimitResult;
import com.throttlix.core.RateLimiter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TokenBucketRateLimiter implements RateLimiter {

    private final ConcurrentMap<String, TokenBucket> clientBuckets = new ConcurrentHashMap<>();

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
        TokenBucket bucket = clientBuckets.get(clientId);

        if (bucket == null) {
            TokenBucket newBucket = new TokenBucket(
                    policy.getLimit(),
                    policy.getRefillTokens(),
                    policy.getRefillDurationMillis(),
                    currentTimeMillis
            );
            TokenBucket existing = clientBuckets.putIfAbsent(clientId, newBucket);
            bucket = (existing != null) ? existing : newBucket;
        }

        synchronized (bucket) {
            boolean requestAllowed = bucket.tryConsume(currentTimeMillis);
            double remainingTokens = bucket.getAvailableTokens();

            if (requestAllowed) {
                return RateLimitResult.allow(remainingTokens, "Request allowed by Token Bucket");
            }

            long retryAfterMillis = bucket.calculateRetryAfterMillis(currentTimeMillis);
            return RateLimitResult.reject(remainingTokens, retryAfterMillis, "Token Bucket rate limit exceeded");
        }
    }
}
