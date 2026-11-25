package com.throttlix.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitPolicyTest {

    @Test
    void shouldCreateTokenBucketPolicySuccessfully() {
        RateLimitPolicy policy = RateLimitPolicy.createTokenBucketPolicy(
                "pol-tb-1", "client-tb", 10, 2, 1000L
        );

        assertEquals("pol-tb-1", policy.getPolicyId());
        assertEquals("client-tb", policy.getClientId());
        assertEquals(AlgorithmType.TOKEN_BUCKET, policy.getAlgorithmType());
        assertEquals(10, policy.getLimit());
        assertEquals(2, policy.getRefillTokens());
        assertEquals(1000L, policy.getRefillDurationMillis());
        assertEquals(0L, policy.getWindowDurationMillis());
        assertTrue(policy.isEnabled());
        assertTrue(policy.toString().contains("pol-tb-1"));
    }

    @Test
    void shouldCreateSlidingWindowPolicySuccessfully() {
        RateLimitPolicy policy = RateLimitPolicy.createSlidingWindowPolicy(
                "pol-sw-1", "client-sw", 50, 60000L
        );

        assertEquals("pol-sw-1", policy.getPolicyId());
        assertEquals("client-sw", policy.getClientId());
        assertEquals(AlgorithmType.SLIDING_WINDOW, policy.getAlgorithmType());
        assertEquals(50, policy.getLimit());
        assertEquals(60000L, policy.getWindowDurationMillis());
        assertEquals(0, policy.getRefillTokens());
        assertEquals(0L, policy.getRefillDurationMillis());
        assertTrue(policy.isEnabled());
        assertTrue(policy.toString().contains("pol-sw-1"));
    }

    @Test
    void shouldRejectInvalidPolicyParameters() {
        // Missing policy ID
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy("", "client-1", AlgorithmType.TOKEN_BUCKET, 10, 0, 1, 1000, true));

        // Missing client ID
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy("pol-1", "   ", AlgorithmType.TOKEN_BUCKET, 10, 0, 1, 1000, true));

        // Null algorithm
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy("pol-1", "client-1", null, 10, 0, 1, 1000, true));

        // Zero or negative limit
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy("pol-1", "client-1", AlgorithmType.TOKEN_BUCKET, 0, 0, 1, 1000, true));
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy("pol-1", "client-1", AlgorithmType.TOKEN_BUCKET, -5, 0, 1, 1000, true));

        // Sliding window with non-positive window duration
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy("pol-1", "client-1", AlgorithmType.SLIDING_WINDOW, 10, 0, 0, 0, true));

        // Token bucket with non-positive refill tokens or duration
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy("pol-1", "client-1", AlgorithmType.TOKEN_BUCKET, 10, 0, 0, 1000, true));
        assertThrows(IllegalArgumentException.class, () ->
                new RateLimitPolicy("pol-1", "client-1", AlgorithmType.TOKEN_BUCKET, 10, 0, 1, 0, true));
    }
}
