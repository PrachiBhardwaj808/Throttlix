package com.throttlix;

import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.core.RateLimitResult;

/**
 * Entry point for the Throttlix console-based application.
 * Demonstrates the shared core models (RateLimitPolicy and RateLimitResult).
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  Throttlix — API Traffic Management Platform     ");
        System.out.println("  Core Foundation Demo (Java 21)                  ");
        System.out.println("==================================================");

        // 1. Demonstrate creating a Token Bucket policy (Prachi's algorithm)
        RateLimitPolicy tokenBucketPolicy = RateLimitPolicy.createTokenBucketPolicy(
                "policy-tb-standard",
                "client-acme-corp",
                10,     // bucket capacity (limit)
                2,      // 2 tokens added per refill
                1000L   // every 1000 ms (1 second)
        );
        System.out.println("\n[1] Created Token Bucket Policy:");
        System.out.println("    Policy ID:        " + tokenBucketPolicy.getPolicyId());
        System.out.println("    Client ID:        " + tokenBucketPolicy.getClientId());
        System.out.println("    Algorithm:        " + tokenBucketPolicy.getAlgorithmType());
        System.out.println("    Bucket Capacity:  " + tokenBucketPolicy.getLimit() + " tokens");
        System.out.println("    Refill Rate:      " + tokenBucketPolicy.getRefillTokens() + " tokens per "
                + tokenBucketPolicy.getRefillDurationMillis() + " ms");
        System.out.println("    Active/Enabled:   " + tokenBucketPolicy.isEnabled());

        // 2. Demonstrate creating a Sliding Window policy (Akshit's algorithm)
        RateLimitPolicy slidingWindowPolicy = RateLimitPolicy.createSlidingWindowPolicy(
                "policy-sw-strict",
                "client-fintech-partner",
                100,    // max 100 requests (limit)
                60000L  // within a 60-second (60,000 ms) window
        );
        System.out.println("\n[2] Created Sliding Window Policy:");
        System.out.println("    Policy ID:        " + slidingWindowPolicy.getPolicyId());
        System.out.println("    Client ID:        " + slidingWindowPolicy.getClientId());
        System.out.println("    Algorithm:        " + slidingWindowPolicy.getAlgorithmType());
        System.out.println("    Request Limit:    " + slidingWindowPolicy.getLimit() + " requests");
        System.out.println("    Window Duration:  " + slidingWindowPolicy.getWindowDurationMillis() + " ms");
        System.out.println("    Active/Enabled:   " + slidingWindowPolicy.isEnabled());

        // 3. Demonstrate an ALLOWED RateLimitResult
        RateLimitResult allowedResult = RateLimitResult.allow(9.0, "Request within permitted quota");
        System.out.println("\n[3] Sample Allowed Result:");
        System.out.println("    Allowed:            " + allowedResult.isAllowed());
        System.out.println("    Remaining Capacity: " + allowedResult.getRemainingCapacity());
        System.out.println("    Retry After Delay:  " + allowedResult.getRetryAfterMillis());
        System.out.println("    Message:            " + allowedResult.getMessage());

        // 4. Demonstrate a REJECTED RateLimitResult
        RateLimitResult rejectedResult = RateLimitResult.reject(0.0, 500L, "Rate limit exceeded. Bucket empty.");
        System.out.println("\n[4] Sample Rejected Result:");
        System.out.println("    Allowed:            " + rejectedResult.isAllowed());
        System.out.println("    Remaining Capacity: " + rejectedResult.getRemainingCapacity());
        System.out.println("    Retry After Delay:  " + rejectedResult.getRetryAfterMillis() + " ms");
        System.out.println("    Message:            " + rejectedResult.getMessage());

        System.out.println("\n==================================================");
        System.out.println("  Foundation models verified successfully!        ");
        System.out.println("==================================================");
    }
}
