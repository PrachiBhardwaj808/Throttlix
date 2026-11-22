package com.throttlix.algorithm.slidingwindow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.core.RateLimitResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SlidingWindowTest {

    @Test
    void allowsRequestsUpToLimitAndReportsRemainingCapacity() {
        SlidingWindow limiter = new SlidingWindow();
        RateLimitPolicy policy = policy("client-a", 3, 10_000);

        RateLimitResult first = limiter.allowRequest(policy, 0);
        RateLimitResult second = limiter.allowRequest(policy, 2_000);
        RateLimitResult third = limiter.allowRequest(policy, 4_000);
        RateLimitResult fourth = limiter.allowRequest(policy, 5_000);

        assertTrue(first.isAllowed());
        assertEquals(2.0, first.getRemainingCapacity());
        assertTrue(second.isAllowed());
        assertEquals(1.0, second.getRemainingCapacity());
        assertTrue(third.isAllowed());
        assertEquals(0.0, third.getRemainingCapacity());
        assertFalse(fourth.isAllowed());
        assertEquals(5_000L, fourth.getRetryAfterMillis());
    }

    @Test
    void expiresRequestAtExactWindowBoundary() {
        SlidingWindow limiter = new SlidingWindow();
        RateLimitPolicy policy = policy("client-a", 1, 10_000);

        assertTrue(limiter.allowRequest(policy, 100).isAllowed());
        assertTrue(limiter.allowRequest(policy, 10_100).isAllowed());
    }

    @Test
    void keepsRequestsInsideWindowAndRetriesWhenOldestExpires() {
        SlidingWindow limiter = new SlidingWindow();
        RateLimitPolicy policy = policy("client-a", 2, 10_000);

        assertTrue(limiter.allowRequest(policy, 1_000).isAllowed());
        assertTrue(limiter.allowRequest(policy, 2_000).isAllowed());
        RateLimitResult rejected = limiter.allowRequest(policy, 3_000);
        assertFalse(rejected.isAllowed());
        assertEquals(8_000L, rejected.getRetryAfterMillis());
        assertTrue(limiter.allowRequest(policy, 11_000).isAllowed());
    }

    @Test
    void maintainsIndependentStatePerClient() {
        SlidingWindow limiter = new SlidingWindow();

        assertTrue(limiter.allowRequest(policy("client-a", 1, 10_000), 100).isAllowed());
        assertTrue(limiter.allowRequest(policy("client-b", 1, 10_000), 100).isAllowed());
        assertFalse(limiter.allowRequest(policy("client-a", 1, 10_000), 200).isAllowed());
    }

    @Test
    void clampsBackwardsTimeToPreserveTimestampOrder() {
        SlidingWindow limiter = new SlidingWindow();
        RateLimitPolicy policy = policy("client-a", 2, 10_000);

        assertTrue(limiter.allowRequest(policy, 5_000).isAllowed());
        RateLimitResult second = limiter.allowRequest(policy, 4_000);

        assertTrue(second.isAllowed());
        assertEquals(0.0, second.getRemainingCapacity());
        assertFalse(limiter.allowRequest(policy, 4_001).isAllowed());
    }

    @Test
    void rejectsNullDisabledAndWrongAlgorithmPolicies() {
        SlidingWindow limiter = new SlidingWindow();
        RateLimitResult nullResult = limiter.allowRequest(null, 0);
        RateLimitPolicy disabled = new RateLimitPolicy(
                "disabled", "client-a", AlgorithmType.SLIDING_WINDOW,
                1, 10_000, 0, 0, false);
        RateLimitPolicy tokenBucket = RateLimitPolicy.createTokenBucketPolicy(
                "token", "client-a", 1, 1, 1_000);

        assertFalse(nullResult.isAllowed());
        assertNull(nullResult.getRetryAfterMillis());
        assertFalse(limiter.allowRequest(disabled, 0).isAllowed());
        assertFalse(limiter.allowRequest(tokenBucket, 0).isAllowed());
    }

    @Test
    void threeRequestLimitHoldsForOneHundredSimultaneousRequests() throws Exception {
        assertConcurrentRequestLimit(3);
    }

    @Test
    void oneRequestLimitHoldsForOneHundredSimultaneousRequests() throws Exception {
        assertConcurrentRequestLimit(1);
    }

    private void assertConcurrentRequestLimit(int limit) throws Exception {
        SlidingWindow limiter = new SlidingWindow();
        RateLimitPolicy policy = policy("client-a", limit, 10_000);
        int requestCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowedCount = new AtomicInteger();
        try {
            List<Future<?>> requests = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                requests.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    if (limiter.allowRequest(policy, 1_000).isAllowed()) {
                        allowedCount.incrementAndGet();
                    }
                    return null;
                }));
            }

            ready.await();
            start.countDown();
            for (Future<?> request : requests) {
                request.get();
            }

            assertEquals(limit, allowedCount.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private RateLimitPolicy policy(String clientId, int limit, long windowMillis) {
        return RateLimitPolicy.createSlidingWindowPolicy(
                "sliding-window-test", clientId, limit, windowMillis);
    }
}
