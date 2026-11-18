package com.throttlix;

import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.core.RateLimitResult;

/**
 * Entry point for the Throttlix console-based application.
 * Demonstrates the common core rate-limiting foundation models:
 * - {@link RateLimitPolicy} (Token Bucket & Sliding Window)
 * - {@link RateLimitResult} (Allowed & Rejected decisions)
 * - {@link AlgorithmType}
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("       Throttlix — Intelligent API Traffic Manager        ");
        System.out.println("            Core Foundation Demonstration                 ");
        System.out.println("==========================================================");
        System.out.println();

        // 1. Demonstrate Token Bucket Policy (Prachi's algorithm configuration)
        System.out.println("--- 1. Token Bucket Policy Configuration (Prachi) ---");
        RateLimitPolicy tokenBucketPolicy = RateLimitPolicy.createTokenBucketPolicy(
                "policy-tb-premium",
                "client-analytics-service",
                100,      // Bucket Capacity (burst limit)
                10,       // Refill Tokens
                1000L     // Refill Interval (every 1 second)
        );
        System.out.println("Policy ID:         " + tokenBucketPolicy.getPolicyId());
        System.out.println("Client ID:         " + tokenBucketPolicy.getClientId());
        System.out.println("Algorithm:         " + tokenBucketPolicy.getAlgorithmType());
        System.out.println("Bucket Capacity:   " + tokenBucketPolicy.getLimit() + " tokens");
        System.out.println("Refill Rate:       " + tokenBucketPolicy.getRefillTokens()
                + " tokens / " + tokenBucketPolicy.getRefillDurationMillis() + " ms");
        System.out.println("Status:            " + (tokenBucketPolicy.isEnabled() ? "ENABLED" : "DISABLED"));
        System.out.println("ToString:          " + tokenBucketPolicy);
        System.out.println();

        // 2. Demonstrate Sliding Window Policy (Akshit's algorithm configuration)
        System.out.println("--- 2. Sliding Window Policy Configuration (Akshit) ---");
        RateLimitPolicy slidingWindowPolicy = RateLimitPolicy.createSlidingWindowPolicy(
                "policy-sw-standard",
                "client-mobile-app",
                60,       // Max requests
                60000L    // Window duration: 60,000 ms (1 minute)
        );
        System.out.println("Policy ID:         " + slidingWindowPolicy.getPolicyId());
        System.out.println("Client ID:         " + slidingWindowPolicy.getClientId());
        System.out.println("Algorithm:         " + slidingWindowPolicy.getAlgorithmType());
        System.out.println("Max Requests:      " + slidingWindowPolicy.getLimit() + " requests");
        System.out.println("Window Duration:   " + slidingWindowPolicy.getWindowDurationMillis() + " ms");
        System.out.println("Status:            " + (slidingWindowPolicy.isEnabled() ? "ENABLED" : "DISABLED"));
        System.out.println("ToString:          " + slidingWindowPolicy);
        System.out.println();

        // 3. Demonstrate RateLimitResult - Allowed Request
        System.out.println("--- 3. RateLimitResult: Allowed Request ---");
        RateLimitResult allowedResult = RateLimitResult.allow(99.0, "Request permitted under Token Bucket policy");
        System.out.println("Allowed:            " + allowedResult.isAllowed());
        System.out.println("Remaining Capacity: " + allowedResult.getRemainingCapacity());
        System.out.println("Retry After:        " + allowedResult.getRetryAfterMillis());
        System.out.println("Message:            " + allowedResult.getMessage());
        System.out.println("ToString:           " + allowedResult);
        System.out.println();

        // 4. Demonstrate RateLimitResult - Rejected Request (429 Rate Exceeded)
        System.out.println("--- 4. RateLimitResult: Rejected Request ---");
        RateLimitResult rejectedResult = RateLimitResult.reject(0.0, 500L, "Rate limit exceeded. Bucket is empty.");
        System.out.println("Allowed:            " + rejectedResult.isAllowed());
        System.out.println("Remaining Capacity: " + rejectedResult.getRemainingCapacity());
        System.out.println("Retry After (ms):   " + rejectedResult.getRetryAfterMillis() + " ms");
        System.out.println("Message:            " + rejectedResult.getMessage());
        System.out.println("ToString:           " + rejectedResult);
        System.out.println();

        System.out.println("==========================================================");
        System.out.println("  Core Foundation Verified: All contracts & models ready! ");
        System.out.println("==========================================================");
    }
}
