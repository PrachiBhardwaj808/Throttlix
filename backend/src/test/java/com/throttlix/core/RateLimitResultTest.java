package com.throttlix.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitResultTest {

    @Test
    void shouldCreateAllowedResult() {
        RateLimitResult result = RateLimitResult.allow(4.5, "Allowed successfully");

        assertTrue(result.isAllowed());
        assertEquals(4.5, result.getRemainingCapacity());
        assertEquals(4.5, result.getRemainingTokens());
        assertNull(result.getRetryAfterMillis());
        assertEquals("Allowed successfully", result.getMessage());
        assertTrue(result.toString().contains("allowed=true"));
    }

    @Test
    void shouldCreateRejectedResult() {
        RateLimitResult result = RateLimitResult.reject(0.0, 1500L, "Quota exceeded");

        assertFalse(result.isAllowed());
        assertEquals(0.0, result.getRemainingCapacity());
        assertEquals(0.0, result.getRemainingTokens());
        assertEquals(1500L, result.getRetryAfterMillis());
        assertEquals("Quota exceeded", result.getMessage());
        assertTrue(result.toString().contains("allowed=false"));
        assertTrue(result.toString().contains("1500ms"));
    }

    @Test
    void shouldUseDefaultMessageWhenNullProvided() {
        RateLimitResult allowDefault = new RateLimitResult(true, 5.0, null, null);
        assertEquals("Request allowed", allowDefault.getMessage());

        RateLimitResult rejectDefault = new RateLimitResult(false, 0.0, 1000L, null);
        assertEquals("Rate limit exceeded", rejectDefault.getMessage());
    }
}
