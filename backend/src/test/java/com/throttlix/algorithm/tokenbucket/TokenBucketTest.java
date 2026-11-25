package com.throttlix.algorithm.tokenbucket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBucketTest {

    @Test
    void shouldInitializeWithFullCapacity() {
        TokenBucket bucket = new TokenBucket(10, 2, 1000L, 0L);

        assertEquals(10, bucket.getMaxTokens());
        assertEquals(10, bucket.getCapacity());
        assertEquals(2, bucket.getRefillTokens());
        assertEquals(1000L, bucket.getRefillDurationMillis());
        assertEquals(0L, bucket.getLastRefillTime());
        assertEquals(0L, bucket.getLastRefillTimeMillis());
        assertEquals(10.0, bucket.getAvailableTokens());
    }

    @Test
    void shouldConsumeTokensAndRejectWhenEmpty() {
        TokenBucket bucket = new TokenBucket(2, 1, 10000L, 0L);

        assertTrue(bucket.tryConsume(0L));
        assertEquals(1.0, bucket.getAvailableTokens());

        assertTrue(bucket.tryConsume(0L));
        assertEquals(0.0, bucket.getAvailableTokens());

        assertFalse(bucket.tryConsume(0L));
        assertEquals(0.0, bucket.getAvailableTokens());
    }

    @Test
    void shouldRefillTokensOverTime() {
        TokenBucket bucket = new TokenBucket(5, 2, 1000L, 0L);

        // Consume all 5 tokens at t = 0
        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.tryConsume(0L));
        }
        assertEquals(0.0, bucket.getAvailableTokens());

        // Advance time by 500ms -> 1 token refilled
        bucket.refill(500L);
        assertEquals(1.0, bucket.getAvailableTokens());

        // Advance time by another 500ms (t = 1000ms) -> another token refilled
        bucket.refill(1000L);
        assertEquals(2.0, bucket.getAvailableTokens());
    }

    @Test
    void shouldNeverExceedMaxCapacityOnRefill() {
        TokenBucket bucket = new TokenBucket(3, 10, 1000L, 0L);

        // Consume 1 token
        assertTrue(bucket.tryConsume(0L));
        assertEquals(2.0, bucket.getAvailableTokens());

        // Advance time by 10,000ms (should refill 100 tokens, but capped at 3)
        bucket.refill(10000L);
        assertEquals(3.0, bucket.getAvailableTokens());
    }

    @Test
    void shouldCalculateAccurateRetryAfterTime() {
        TokenBucket bucket = new TokenBucket(1, 1, 1000L, 0L);

        assertTrue(bucket.tryConsume(0L));
        assertFalse(bucket.tryConsume(0L));

        // When 0 tokens available and refill rate is 1 token / 1000ms
        assertEquals(1000L, bucket.calculateRetryAfterMillis(0L));

        // After 400ms has elapsed, 600ms remaining
        assertEquals(600L, bucket.calculateRetryAfterMillis(400L));

        // After 1000ms has elapsed, token is available so retryAfter is 0
        assertEquals(0L, bucket.calculateRetryAfterMillis(1000L));
    }

    @Test
    void shouldRejectInvalidConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(0, 1, 1000L, 0L));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(5, 0, 1000L, 0L));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(5, 1, 0L, 0L));
    }
}
