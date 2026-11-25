package com.throttlix.client;

import java.io.Console;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Scanner;

/**
 * Terminal 2: Throttlix Interactive Client Console.
 * Sends real HTTP requests to the Throttlix Gateway Server using Java 21 HttpClient.
 * Demonstrates the full client-side request lifecycle, authentication header propagation,
 * rate-limiting header inspection, and burst traffic evaluation.
 */
public class ClientConsole {

    // ANSI Colors for Console Output
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";

    public static final String DEFAULT_SERVER_URL = "http://localhost:8080";
    public static final String DEFAULT_ENDPOINT = "/api/products";
    public static final String DEFAULT_METHOD = "GET";

    private final HttpClient httpClient;
    private final Scanner scanner;
    private String serverUrl = DEFAULT_SERVER_URL;
    private String apiKey = null;
    private String endpoint = DEFAULT_ENDPOINT;
    private String httpMethod = DEFAULT_METHOD;

    public ClientConsole() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        ClientConsole client = new ClientConsole();
        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            client.serverUrl = args[0].trim();
        }
        client.runInteractiveLoop();
    }

    public void runInteractiveLoop() {
        boolean running = true;

        while (running) {
            printMainMenu();
            System.out.print(CYAN + BOLD + "Select Option (1-8): " + RESET);
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1" -> sendSingleRequest();
                case "2" -> sendMultipleRequests();
                case "3" -> configureApiKey();
                case "4" -> changeEndpointAndMethod();
                case "5" -> viewConfiguration();
                case "6" -> selectDemoApiKey();
                case "7" -> changeServerUrl();
                case "8", "exit", "quit" -> {
                    System.out.println(CYAN + "\nExiting Throttlix API Client. Goodbye!" + RESET);
                    running = false;
                }
                default -> System.out.println(RED + "Invalid option. Please choose between 1 and 8." + RESET);
            }
        }
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println(CYAN + BOLD + "================================================================================" + RESET);
        System.out.println(CYAN + BOLD + "                             THROTTLIX API CLIENT                               " + RESET);
        System.out.println(CYAN + "                       Intelligent API Traffic Manager                          " + RESET);
        System.out.println(CYAN + BOLD + "================================================================================" + RESET);
        System.out.println("  Target Server   : " + BOLD + serverUrl + RESET);
        System.out.println("  Active Endpoint : " + BOLD + endpoint + RESET + " [" + httpMethod + "]");
        System.out.println("  Active API Key  : " + (apiKey != null ? GREEN + maskApiKey(apiKey) + RESET : YELLOW + "[NOT CONFIGURED]" + RESET));
        System.out.println(CYAN + "--------------------------------------------------------------------------------" + RESET);
        System.out.println("  1. Send API Request");
        System.out.println("  2. Send Multiple Requests (Traffic Burst Simulation)");
        System.out.println("  3. Configure API Key (Secure Input)");
        System.out.println("  4. Change API Endpoint / HTTP Method");
        System.out.println("  5. View Request Configuration");
        System.out.println("  6. Quick Select Demo Keys (Token Bucket / Sliding Window / Invalid)");
        System.out.println("  7. Change Target Server URL");
        System.out.println("  8. Exit");
        System.out.println(CYAN + "================================================================================" + RESET);
    }

    private void sendSingleRequest() {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println(YELLOW + "\n[WARNING] No API key is currently configured." + RESET);
            System.out.print("Send request without API key to test 401 Unauthorized? (y/n): ");
            String ans = scanner.nextLine().trim();
            if (!ans.equalsIgnoreCase("y")) {
                configureApiKey();
                if (apiKey == null || apiKey.isBlank()) {
                    return;
                }
            }
        }

        System.out.println();
        System.out.println(CYAN + "[DISPATCHING REQUEST] -> " + httpMethod + " " + serverUrl + endpoint + RESET);

        long startTime = System.currentTimeMillis();
        try {
            HttpRequest request = buildHttpRequest();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - startTime;

            displaySingleResponse(response, latency);
        } catch (IOException | InterruptedException e) {
            System.out.println(RED + "\n[CONNECTION FAILED] Unable to reach Gateway Server at " + serverUrl + RESET);
            System.out.println(RED + "Error details: " + e.getMessage() + RESET);
            System.out.println(YELLOW + "Make sure Terminal 1 (ServerConsole) is running." + RESET);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void sendMultipleRequests() {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println(YELLOW + "\n[WARNING] No API key is configured. Please select or enter a key first." + RESET);
            selectDemoApiKey();
            if (apiKey == null) {
                return;
            }
        }

        System.out.print("\nEnter number of requests to send (e.g. 5, 10, 15): ");
        int totalRequests;
        try {
            totalRequests = Integer.parseInt(scanner.nextLine().trim());
            if (totalRequests <= 0) {
                System.out.println(RED + "Number of requests must be positive." + RESET);
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println(RED + "Invalid number format." + RESET);
            return;
        }

        System.out.print("Enter delay between requests in milliseconds (0 for immediate burst, or e.g. 200): ");
        long delayMs = 0;
        try {
            String delayInput = scanner.nextLine().trim();
            if (!delayInput.isEmpty()) {
                delayMs = Long.parseLong(delayInput);
            }
        } catch (NumberFormatException e) {
            System.out.println(YELLOW + "Invalid delay format, defaulting to 0 ms." + RESET);
            delayMs = 0;
        }

        System.out.println();
        System.out.println(CYAN + BOLD + "===========================================================================================" + RESET);
        System.out.println(CYAN + BOLD + "                        EXECUTING BURST TRAFFIC SIMULATION                                 " + RESET);
        System.out.println(CYAN + BOLD + "===========================================================================================" + RESET);
        System.out.println("  Target Endpoint : " + BOLD + httpMethod + " " + serverUrl + endpoint + RESET);
        System.out.println("  Total Requests  : " + totalRequests);
        System.out.println("  Inter-Delay     : " + delayMs + " ms");
        System.out.println("  API Key Used    : " + maskApiKey(apiKey));
        System.out.println(CYAN + "-------------------------------------------------------------------------------------------" + RESET);
        System.out.printf("%-4s %-6s %-18s %-14s %-12s %-14s %-10s %-12s%n",
                "#", "METHOD", "ENDPOINT", "HTTP STATUS", "REMAINING", "RETRY-AFTER", "LATENCY", "DECISION");
        System.out.println(CYAN + "-------------------------------------------------------------------------------------------" + RESET);

        int allowedCount = 0;
        int rateLimitedCount = 0;
        int errorCount = 0;
        long totalLatency = 0;

        for (int i = 1; i <= totalRequests; i++) {
            long reqStart = System.currentTimeMillis();
            try {
                HttpRequest request = buildHttpRequest();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                long latency = System.currentTimeMillis() - reqStart;
                totalLatency += latency;

                int statusCode = response.statusCode();
                String remainingHeader = response.headers().firstValue("X-RateLimit-Remaining").orElse("-");
                String retryAfterHeader = response.headers().firstValue("Retry-After").orElse("-");

                String statusColor;
                String decision;

                if (statusCode == 200) {
                    statusColor = GREEN;
                    decision = "ALLOWED";
                    allowedCount++;
                } else if (statusCode == 429) {
                    statusColor = YELLOW;
                    decision = "RATE LIMITED";
                    rateLimitedCount++;
                } else {
                    statusColor = RED;
                    decision = "REJECTED (" + statusCode + ")";
                    errorCount++;
                }

                System.out.printf("%-4d %-6s %-18s " + statusColor + "%-14s" + RESET + " %-12s %-14s %-10s " + statusColor + "%-12s" + RESET + "%n",
                        i,
                        httpMethod,
                        endpoint,
                        statusCode + " " + getReasonPhrase(statusCode),
                        remainingHeader,
                        (retryAfterHeader.equals("-") ? "-" : retryAfterHeader + " s"),
                        latency + " ms",
                        decision
                );

                if (delayMs > 0 && i < totalRequests) {
                    Thread.sleep(delayMs);
                }
            } catch (IOException | InterruptedException e) {
                System.out.printf("%-4d %-6s %-18s " + RED + "%-14s" + RESET + " %-12s %-14s %-10s " + RED + "%-12s" + RESET + "%n",
                        i, httpMethod, endpoint, "ERROR", "-", "-", "-", "FAILED");
                errorCount++;
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        System.out.println(CYAN + "-------------------------------------------------------------------------------------------" + RESET);
        System.out.println(BOLD + "[BURST EXECUTION SUMMARY]" + RESET);
        System.out.println("  Total Requests Sent : " + totalRequests);
        System.out.println("  Allowed (200 OK)    : " + GREEN + allowedCount + RESET);
        System.out.println("  Rate Limited (429)  : " + YELLOW + rateLimitedCount + RESET);
        System.out.println("  Other / Errors      : " + (errorCount > 0 ? RED + errorCount + RESET : "0"));
        if (totalRequests > 0) {
            double avgLatency = (double) totalLatency / totalRequests;
            System.out.printf("  Average Latency     : %.1f ms%n", avgLatency);
        }
        System.out.println(CYAN + "===========================================================================================" + RESET);
    }

    private void displaySingleResponse(HttpResponse<String> response, long latency) {
        int status = response.statusCode();
        String statusColor = status == 200 ? GREEN : (status == 429 ? YELLOW : RED);

        String limitHeader = response.headers().firstValue("X-RateLimit-Limit").orElse("-");
        String remainingHeader = response.headers().firstValue("X-RateLimit-Remaining").orElse("-");
        String retryAfterHeader = response.headers().firstValue("Retry-After").orElse("-");

        System.out.println();
        System.out.println(CYAN + "--------------------------------------------------------------------------------" + RESET);
        System.out.println(BOLD + "[GATEWAY HTTP RESPONSE]" + RESET);
        System.out.println("  Status Code      : " + statusColor + BOLD + status + " " + getReasonPhrase(status) + RESET);
        System.out.println("  Endpoint         : " + endpoint);
        System.out.println("  Response Time    : " + latency + " ms");
        System.out.println("  RateLimit-Limit  : " + limitHeader);
        System.out.println("  Remaining Limit  : " + remainingHeader);
        System.out.println("  Retry-After      : " + (retryAfterHeader.equals("-") ? "-" : retryAfterHeader + " seconds"));
        System.out.println();
        System.out.println(BOLD + "  Response Body:" + RESET);
        System.out.println(DIM + "  " + response.body() + RESET);
        System.out.println(CYAN + "--------------------------------------------------------------------------------" + RESET);
    }

    private void configureApiKey() {
        System.out.println();
        System.out.println(CYAN + "[SECURE API KEY INPUT]" + RESET);
        System.out.print("Enter API Key: ");

        Console console = System.console();
        String key;
        if (console != null) {
            char[] secret = console.readPassword();
            key = new String(secret).trim();
        } else {
            key = scanner.nextLine().trim();
        }

        if (key.isEmpty()) {
            System.out.println(YELLOW + "API key cleared." + RESET);
            this.apiKey = null;
        } else {
            this.apiKey = key;
            System.out.println(GREEN + "API key accepted: " + maskApiKey(this.apiKey) + RESET);
            System.out.println(DIM + "(Raw API key is masked and will be transmitted via X-API-Key header)" + RESET);
        }
    }

    private void selectDemoApiKey() {
        System.out.println();
        System.out.println(CYAN + BOLD + "[QUICK DEMO API KEYS]" + RESET);
        System.out.println("  1. Token Bucket Client   : thx-test-key-1 (Capacity: 10 tokens, Refill: 5/sec)");
        System.out.println("  2. Sliding Window Client : thx-test-key-2 (Limit: 5 requests / 10 sec)");
        System.out.println("  3. Invalid Key Test      : thx-invalid-key-xyz (Tests 401 Unauthorized)");
        System.out.println("  4. Custom Key Entry      : Type your own key");
        System.out.print("Select Key (1-4): ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> {
                this.apiKey = "thx-test-key-1";
                System.out.println(GREEN + "Active Key: thx-test-key-1 (Token Bucket - client-analytics)" + RESET);
            }
            case "2" -> {
                this.apiKey = "thx-test-key-2";
                System.out.println(GREEN + "Active Key: thx-test-key-2 (Sliding Window - client-mobile)" + RESET);
            }
            case "3" -> {
                this.apiKey = "thx-invalid-key-xyz";
                System.out.println(YELLOW + "Active Key: thx-invalid-key-xyz (Simulated unauthorized key)" + RESET);
            }
            case "4" -> configureApiKey();
            default -> System.out.println(RED + "Invalid choice. Key unchanged." + RESET);
        }
    }

    private void changeEndpointAndMethod() {
        System.out.println();
        System.out.println(CYAN + BOLD + "[ENDPOINT CONFIGURATION]" + RESET);
        System.out.println("  1. /api/products  (Protected Business API)");
        System.out.println("  2. /api/users     (Protected Business API)");
        System.out.println("  3. /api/limit     (Direct Rate Limiting Telemetry)");
        System.out.println("  4. /api/health    (Public Gateway Health Check)");
        System.out.println("  5. Custom Endpoint");
        System.out.print("Choose endpoint (1-5): ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> this.endpoint = "/api/products";
            case "2" -> this.endpoint = "/api/users";
            case "3" -> this.endpoint = "/api/limit";
            case "4" -> this.endpoint = "/api/health";
            case "5" -> {
                System.out.print("Enter custom URI path (e.g. /api/orders): ");
                String custom = scanner.nextLine().trim();
                if (!custom.startsWith("/")) {
                    custom = "/" + custom;
                }
                this.endpoint = custom;
            }
            default -> System.out.println(YELLOW + "Invalid choice, keeping " + this.endpoint + RESET);
        }

        System.out.print("Select HTTP Method (1. GET, 2. POST) [current: " + httpMethod + "]: ");
        String mChoice = scanner.nextLine().trim();
        if ("2".equals(mChoice) || "POST".equalsIgnoreCase(mChoice)) {
            this.httpMethod = "POST";
        } else if ("1".equals(mChoice) || "GET".equalsIgnoreCase(mChoice)) {
            this.httpMethod = "GET";
        }

        System.out.println(GREEN + "Endpoint updated to: " + httpMethod + " " + endpoint + RESET);
    }

    private void viewConfiguration() {
        System.out.println();
        System.out.println(CYAN + BOLD + "================================================================================" + RESET);
        System.out.println(CYAN + BOLD + "                       CURRENT REQUEST CONFIGURATION                            " + RESET);
        System.out.println(CYAN + BOLD + "================================================================================" + RESET);
        System.out.println("  Target Server   : " + serverUrl);
        System.out.println("  Endpoint Path   : " + endpoint);
        System.out.println("  HTTP Method     : " + httpMethod);
        System.out.println("  Header Sent     : X-API-Key: " + (apiKey != null ? maskApiKey(apiKey) : "[None]"));
        System.out.println("  Full Target URL : " + serverUrl + endpoint);
        System.out.println(CYAN + "================================================================================" + RESET);
    }

    private void changeServerUrl() {
        System.out.print("\nEnter new Gateway Server URL (e.g. http://localhost:8080): ");
        String url = scanner.nextLine().trim();
        if (!url.isEmpty()) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            this.serverUrl = url;
            System.out.println(GREEN + "Target Server URL updated to: " + serverUrl + RESET);
        }
    }

    private HttpRequest buildHttpRequest() {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + endpoint))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json");

        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("X-API-Key", apiKey.trim());
        }

        if ("POST".equalsIgnoreCase(httpMethod)) {
            builder.POST(HttpRequest.BodyPublishers.ofString("{}"));
            builder.header("Content-Type", "application/json");
        } else {
            builder.GET();
        }

        return builder.build();
    }

    private static String maskApiKey(String key) {
        if (key == null || key.isBlank()) {
            return "[NONE]";
        }
        String clean = key.trim();
        if (clean.length() <= 6) {
            return "***";
        }
        return clean.substring(0, 4) + "****" + clean.substring(clean.length() - 2);
    }

    private static String getReasonPhrase(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            default -> "HTTP " + statusCode;
        };
    }
}
