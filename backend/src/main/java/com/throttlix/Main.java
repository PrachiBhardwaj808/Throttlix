package com.throttlix;

import com.throttlix.algorithm.slidingwindow.SlidingWindow;
import com.throttlix.algorithm.tokenbucket.TokenBucketRateLimiter;
import com.throttlix.client.ClientConsole;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.core.RateLimitResult;
import com.throttlix.server.ServerConsole;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Throttlix Main Launcher.
 * Provides a unified entry point to:
 * 1. Start the Throttlix Gateway Server (Terminal 1)
 * 2. Launch the Throttlix API Client Console (Terminal 2)
 * 3. Run the standalone algorithm demonstration
 */
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
        if (args.length > 0) {
            String command = args[0].trim().toLowerCase();
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

            switch (command) {
                case "server", "gateway" -> {
                    ServerConsole.main(subArgs);
                    return;
                }
                case "client" -> {
                    ClientConsole.main(subArgs);
                    return;
                }
                case "demo", "benchmark" -> {
                    runAlgorithmDemo();
                    return;
                }
                default -> {
                    // Fall through to interactive menu
                }
            }
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println();
        System.out.println(CYAN + BOLD + "================================================================================" + RESET);
        System.out.println(CYAN + BOLD + "                  THROTTLIX — INTELLIGENT API TRAFFIC MANAGER                   " + RESET);
        System.out.println(CYAN + "                     Unified Gateway Demonstration Platform                     " + RESET);
        System.out.println(CYAN + BOLD + "================================================================================" + RESET);
        System.out.println("  1. " + GREEN + BOLD + "Start Throttlix Gateway Server" + RESET + " (Terminal 1 - Core Backend Engine)");
        System.out.println("  2. " + CYAN + BOLD + "Launch Throttlix API Client" + RESET + " (Terminal 2 - Interactive Traffic Client)");
        System.out.println("  3. " + YELLOW + BOLD + "Run Standalone Algorithm Demo" + RESET + " (Token Bucket & Sliding Window Simulation)");
        System.out.println("  4. Exit");
        System.out.println(CYAN + "================================================================================" + RESET);
        System.out.print(CYAN + BOLD + "Select an option (1-4): " + RESET);

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> ServerConsole.main(new String[0]);
            case "2" -> ClientConsole.main(new String[0]);
            case "3" -> runAlgorithmDemo();
            case "4", "exit", "quit" -> System.out.println(CYAN + "Exiting Throttlix. Goodbye!" + RESET);
            default -> {
                System.out.println(YELLOW + "Invalid choice. Launching default algorithm demonstration..." + RESET);
                runAlgorithmDemo();
            }
        }
    }

    public static void runAlgorithmDemo() {
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

        RateLimitResult tbResult = tokenBucketLimiter.allowRequest(tbPolicy, startTime + 1000L);
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

        RateLimitResult swResult = slidingWindowLimiter.allowRequest(swPolicy, startTime + 5000L);
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
                "REQUEST", "TIME", "STATUS", "REMAINING", "RETRY");
        printDivider();
    }

    private static void printRequest(int requestNumber, String time, RateLimitResult result) {
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
        System.out.println(GREEN + BOLD + "  DEMONSTRATION COMPLETED" + RESET);
        System.out.println(DIM + "  Token Bucket and Sliding Window executed successfully." + RESET);
        printDivider();
        System.out.println();
    }
}
