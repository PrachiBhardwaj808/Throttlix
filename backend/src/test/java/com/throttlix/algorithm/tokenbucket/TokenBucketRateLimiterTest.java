package com.throttlix.algorithm.tokenbucket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.core.RateLimitResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

public class TokenBucketRateLimiterTest {

    private RateLimitPolicy createPolicy(String clientId, int capacity, int refillTokens, long refillDurationMillis) {
        return RateLimitPolicy.createTokenBucketPolicy(
                "test-policy-" + clientId,
                clientId,
                capacity,
                refillTokens,
                refillDurationMillis
        );
    }

    @Test
    public void testRequestAllowedWhenTokensAvailable() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        RateLimitPolicy policy = createPolicy("client-1", 5, 2, 1000L);

        RateLimitResult result = limiter.allowRequest(policy, 0L);

        assertTrue(result.isAllowed());
        assertEquals(4.0, result.getRemainingCapacity());
        assertNull(result.getRetryAfterMillis());
    }

    @Test
    public void testRequestRejectedWhenBucketIsEmpty() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        RateLimitPolicy policy = createPolicy("client-1", 3, 1, 1000L);

        assertTrue(limiter.allowRequest(policy, 0L).isAllowed());
        assertTrue(limiter.allowRequest(policy, 0L).isAllowed());
        assertTrue(limiter.allowRequest(policy, 0L).isAllowed());

        RateLimitResult rejectedResult = limiter.allowRequest(policy, 0L);

        assertFalse(rejectedResult.isAllowed());
        assertEquals(0.0, rejectedResult.getRemainingCapacity());
        assertNotNull(rejectedResult.getRetryAfterMillis());
        assertTrue(rejectedResult.getRetryAfterMillis() > 0);
    }

    @Test
    public void testTokenRefillAfterConfiguredDuration() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        RateLimitPolicy policy = createPolicy("client-1", 2, 2, 1000L);

        assertTrue(limiter.allowRequest(policy, 0L).isAllowed());
        assertTrue(limiter.allowRequest(policy, 0L).isAllowed());
        assertFalse(limiter.allowRequest(policy, 0L).isAllowed());

        RateLimitResult afterRefill = limiter.allowRequest(policy, 1000L);

        assertTrue(afterRefill.isAllowed());
        assertEquals(1.0, afterRefill.getRemainingCapacity());
    }

    @Test
    public void testBucketCapacityNeverExceeded() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        RateLimitPolicy policy = createPolicy("client-1", 5, 2, 1000L);

        limiter.allowRequest(policy, 0L);

        RateLimitResult result = limiter.allowRequest(policy, 10000L);

        assertTrue(result.isAllowed());
        assertEquals(4.0, result.getRemainingCapacity());
    }

    @Test
    public void testRemainingCapacityCalculation() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        RateLimitPolicy policy = createPolicy("client-1", 3, 1, 5000L);

        RateLimitResult r1 = limiter.allowRequest(policy, 0L);
        RateLimitResult r2 = limiter.allowRequest(policy, 0L);
        RateLimitResult r3 = limiter.allowRequest(policy, 0L);

        assertEquals(2.0, r1.getRemainingCapacity());
        assertEquals(1.0, r2.getRemainingCapacity());
        assertEquals(0.0, r3.getRemainingCapacity());
    }

    @Test
    public void testRetryAfterCalculation() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        RateLimitPolicy policy = createPolicy("client-1", 1, 2, 1000L);

        assertTrue(limiter.allowRequest(policy, 0L).isAllowed());

        RateLimitResult rejectedAtZero = limiter.allowRequest(policy, 0L);
        assertFalse(rejectedAtZero.isAllowed());
        assertEquals(500L, rejectedAtZero.getRetryAfterMillis());

        RateLimitResult rejectedAt250 = limiter.allowRequest(policy, 250L);
        assertFalse(rejectedAt250.isAllowed());
        assertEquals(250L, rejectedAt250.getRetryAfterMillis());
    }

    @Test
    public void testDifferentClientsHaveIndependentBuckets() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        RateLimitPolicy clientA = createPolicy("client-a", 1, 1, 1000L);
        RateLimitPolicy clientB = createPolicy("client-b", 1, 1, 1000L);

        assertTrue(limiter.allowRequest(clientA, 0L).isAllowed());
        assertFalse(limiter.allowRequest(clientA, 0L).isAllowed());

        assertTrue(limiter.allowRequest(clientB, 0L).isAllowed());
    }

    @Test
    public void testDisabledPolicyAllowsRequests() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        RateLimitPolicy disabledPolicy = new RateLimitPolicy(
                "disabled-policy",
                "client-disabled",
                AlgorithmType.TOKEN_BUCKET,
                5,
                0L,
                1,
                1000L,
                false
        );

        for (int i = 0; i < 10; i++) {
            RateLimitResult result = limiter.allowRequest(disabledPolicy, 0L);
            assertTrue(result.isAllowed());
        }
    }

    @Test
    public void testMultipleSimultaneousRequestsDoNotExceedLimit() throws Exception {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        int capacity = 10;
        RateLimitPolicy policy = createPolicy("client-concurrent", capacity, 1, 100000L);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            tasks.add(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    return limiter.allowRequest(policy, 0L).isAllowed();
                }
            });
        }

        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        int allowedCount = 0;
        int rejectedCount = 0;

        for (Future<Boolean> future : futures) {
            if (future.get()) {
                allowedCount++;
            } else {
                rejectedCount++;
            }
        }

        assertEquals(10, allowedCount);
        assertEquals(40, rejectedCount);
    }
}
