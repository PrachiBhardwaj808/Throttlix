
package com.throttlix;

import com.throttlix.algorithm.slidingwindow.SlidingWindow;
import com.throttlix.algorithm.tokenbucket.TokenBucketRateLimiter;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.core.RateLimitResult;

public class Main {

        private static final String RESET = "\u001B[0m";
        private static final String CYAN = "\u001B[36m";
        private static final String GREEN = "\u001B[32m";
        private static final String RED = "\u001B[31m";
        private static final String YELLOW = "\u001B[33m";
        private static final String BOLD = "\u001B[1m";
        private static final String DIM = "\u001B[2m";

        private static final int WIDTH = 76;

        public static void main(String[] args) {

                long startTime = 1000L;

                printHeader();

                // Token Bucket Demonstration
                TokenBucketRateLimiter tokenBucketLimiter = new TokenBucketRateLimiter();

                RateLimitPolicy tbPolicy = RateLimitPolicy.createTokenBucketPolicy(
                                "policy-tb-standard",
                                "client-analytics",
                                3,
                                1,
                                1000L);

                printSection("TOKEN BUCKET", "Prachi");
                printConfiguration("Capacity", "3 tokens");
                printConfiguration("Refill Rate", "1 token / 1000 ms");

                printTableHeader();

                for (int i = 1; i <= 4; i++) {

                        RateLimitResult result = tokenBucketLimiter.allowRequest(tbPolicy, startTime);

                        printRequest(i, "0 ms", result);
                }

                printEvent("Time advanced by 1000 ms");

                RateLimitResult tbResult = tokenBucketLimiter.allowRequest(
                                tbPolicy,
                                startTime + 1000L);

                printRequest(5, "1000 ms", tbResult);

                printDivider();

                // Sliding Window Demonstration
                SlidingWindow slidingWindowLimiter = new SlidingWindow();

                RateLimitPolicy swPolicy = RateLimitPolicy.createSlidingWindowPolicy(
                                "policy-sw-standard",
                                "client-mobile",
                                2,
                                5000L);

                printSection("SLIDING WINDOW", "Akshit");
                printConfiguration("Request Limit", "2 requests");
                printConfiguration("Window Duration", "5000 ms");

                printTableHeader();

                for (int i = 1; i <= 3; i++) {

                        RateLimitResult result = slidingWindowLimiter.allowRequest(swPolicy, startTime);

                        printRequest(i, "0 ms", result);
                }

                printEvent("Time advanced by 5000 ms");

                RateLimitResult swResult = slidingWindowLimiter.allowRequest(
                                swPolicy,
                                startTime + 5000L);

                printRequest(4, "5000 ms", swResult);

                printFooter();
        }

        private static void printHeader() {

                System.out.println();
                System.out.println(CYAN + BOLD + "  THROTTLIX" + RESET
                                + DIM + "  /  API TRAFFIC MANAGEMENT" + RESET);

                System.out.println(DIM + "  Live Rate Limiting Demonstration" + RESET);

                printDivider();

        }

        private static void printSection(String algorithm, String owner) {

                System.out.println();
                System.out.println(BOLD + CYAN + "  " + algorithm + RESET
                                + DIM + "  |  " + owner + RESET);

                printDivider();
        }

        private static void printConfiguration(String label, String value) {

                System.out.printf("  %-20s : %s%n", label, value);
        }

        private static void printTableHeader() {

                printDivider();

                System.out.printf(
                                BOLD + "  %-12s %-12s %-14s %-12s %-12s%n" + RESET,
                                "REQUEST",
                                "TIME",
                                "STATUS",
                                "REMAINING",
                                "RETRY");

                printDivider();
        }

        private static void printRequest(
                        int requestNumber,
                        String time,
                        RateLimitResult result) {

                String status = result.isAllowed()
                                ? GREEN + "ALLOWED" + RESET
                                : RED + "REJECTED" + RESET;

                String retry = result.getRetryAfterMillis() == null
                                ? "-"
                                : result.getRetryAfterMillis() + " ms";

                String remaining = (result.getRemainingCapacity() == (long) result.getRemainingCapacity())
                                ? String.valueOf((long) result.getRemainingCapacity())
                                : String.format("%.1f", result.getRemainingCapacity());

                System.out.printf(
                                "  %-12s %-12s %-23s %-12s %-12s%n",
                                "#" + requestNumber,
                                time,
                                status,
                                remaining,
                                retry);
        }

        private static void printEvent(String message) {

                System.out.println();
                System.out.println(YELLOW + "  > " + message + RESET);
                System.out.println();
        }

        private static void printDivider() {

                System.out.println(DIM + "  " + "-".repeat(WIDTH - 2) + RESET);
        }

        private static void printFooter() {

                printDivider();

                System.out.println(
                                GREEN + BOLD + "  DEMONSTRATION COMPLETED" + RESET);

                System.out.println(
                                DIM + "  Token Bucket and Sliding Window executed successfully."
                                                + RESET);

                printDivider();
                System.out.println();
        }
}
