package com.throttlix.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.throttlix.auth.ApiKeyAuthService;
import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.core.RateLimitResult;
import com.throttlix.database.DatabaseConfig;
import com.throttlix.service.RateLimiterService;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Embedded HTTP API Gateway Server for Throttlix.
 * Runs on standard Java 21 without external application server dependencies.
 * Dispatches incoming HTTP requests through real authentication and rate limiting pipelines.
 */
public class GatewayServer {

    // ANSI Colors for Console Output
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";

    private final int port;
    private final ApiKeyAuthService authService;
    private final RateLimiterService rateLimiterService;
    private final DatabaseConfig databaseConfig;
    private final boolean verboseLogging;

    private HttpServer server;
    private final AtomicLong requestCounter = new AtomicLong(1000);

    public GatewayServer(int port) {
        this(port, new ApiKeyAuthService(), new RateLimiterService(), new DatabaseConfig(), true);
    }

    public GatewayServer(int port, ApiKeyAuthService authService, RateLimiterService rateLimiterService,
                         DatabaseConfig databaseConfig, boolean verboseLogging) {
        this.port = port;
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
        this.databaseConfig = databaseConfig;
        this.verboseLogging = verboseLogging;
    }

    /**
     * Starts the embedded Gateway HTTP server.
     */
    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Register request handler for all /api/ endpoints
        server.createContext("/api", new GatewayHttpHandler());

        // Use Java 21 virtual threads for high-throughput concurrency
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        if (verboseLogging) {
            printServerStartupBanner();
        }
    }

    /**
     * Stops the embedded Gateway HTTP server.
     */
    public synchronized void stop(int delaySeconds) {
        if (server != null) {
            server.stop(delaySeconds);
            server = null;
            if (verboseLogging) {
                System.out.println(YELLOW + "[GATEWAY SERVER] Server stopped." + RESET);
            }
        }
    }

    public int getPort() {
        if (server != null) {
            return server.getAddress().getPort();
        }
        return port;
    }

    private void printServerStartupBanner() {
        boolean dbConnected = false;
        try {
            dbConnected = databaseConfig.testConnection();
        } catch (Exception ignored) {
            // Handled gracefully below
        }

        System.out.println();
        System.out.println(CYAN + BOLD + "================================================================================" + RESET);
        System.out.println(CYAN + BOLD + "                      THROTTLIX GATEWAY SERVER (v1.0.0)                         " + RESET);
        System.out.println(CYAN + "                     Intelligent API Traffic Management                         " + RESET);
        System.out.println(CYAN + BOLD + "================================================================================" + RESET);
        System.out.println(GREEN + "  [STATUS]    " + RESET + "Gateway Server LIVE on " + BOLD + "http://localhost:" + getPort() + RESET);
        System.out.println(CYAN + "  [ENGINE]    " + RESET + "Java 21 Virtual Threads Dispatcher Active");
        if (dbConnected) {
            System.out.println(GREEN + "  [DATABASE]  " + RESET + "MySQL Connected (" + databaseConfig.getMaskedUrl() + ")");
        } else {
            System.out.println(YELLOW + "  [DATABASE]  " + RESET + "MySQL Offline — Standalone In-Memory Development Store Active");
        }
        System.out.println(CYAN + "  [POLICIES]  " + RESET + "Token Bucket & Sliding Window Algorithms Initialized");
        System.out.println(DIM + "  [ENDPOINTS] " + RESET + "/api/products, /api/users, /api/limit, /api/health");
        System.out.println(DIM + "  [DEMO KEYS] " + RESET + "thx-test-key-1 (Token Bucket), thx-test-key-2 (Sliding Window)");
        System.out.println(CYAN + "--------------------------------------------------------------------------------" + RESET);
        System.out.println(GREEN + "  Waiting for incoming client requests... (Press Ctrl+C to stop)" + RESET);
        System.out.println(CYAN + "================================================================================" + RESET);
        System.out.println();
    }

    private class GatewayHttpHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long startTime = System.currentTimeMillis();
            long requestId = requestCounter.incrementAndGet();
            String method = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();
            String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
            String rawApiKey = exchange.getRequestHeaders().getFirst("X-API-Key");
            String maskedKey = maskApiKey(rawApiKey);
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());

            if (verboseLogging) {
                System.out.println(CYAN + "--------------------------------------------------------------------------------" + RESET);
                System.out.println(BOLD + "[REQUEST RECEIVED]  #REQ-" + requestId + RESET + " at " + timestamp);
                System.out.println("  Method      : " + BOLD + method + RESET);
                System.out.println("  Endpoint    : " + BOLD + path + RESET);
                System.out.println("  Client IP   : " + clientIp);
                System.out.println("  X-API-Key   : " + maskedKey);
            }

            // Public Health Check Endpoint (/api/health)
            if ("/api/health".equals(path)) {
                handleHealthCheck(exchange, requestId, startTime);
                return;
            }

            // Validate HTTP Method
            if (!"GET".equals(method) && !"POST".equals(method)) {
                sendError(exchange, 405, "METHOD_NOT_ALLOWED", "HTTP method not allowed on this gateway endpoint", null, 0, 0);
                if (verboseLogging) {
                    System.out.println(RED + "  [GATEWAY REJECT] Method " + method + " not allowed." + RESET);
                }
                return;
            }

            // Step 1: Authentication
            if (verboseLogging) {
                System.out.println();
                System.out.println(CYAN + "  [STEP 1: AUTHENTICATION]" + RESET);
                System.out.println("  Validating API key credentials...");
            }

            if (rawApiKey == null || rawApiKey.trim().isEmpty()) {
                if (verboseLogging) {
                    System.out.println(RED + "  [AUTH RESULT] FAILED: Missing X-API-Key header" + RESET);
                    System.out.println(RED + "  [GATEWAY ACTION] Request BLOCKED with HTTP 401 Unauthorized" + RESET);
                }
                sendError(exchange, 401, "UNAUTHORIZED", "Missing X-API-Key header", null, 0, 0);
                return;
            }

            String keyHash = ApiKeyAuthService.hashKey(rawApiKey.trim());
            if (verboseLogging) {
                System.out.println("  SHA-256 Hash : " + DIM + (keyHash != null ? keyHash.substring(0, 16) + "..." : "null") + RESET);
            }

            String clientId = authService.authenticate(rawApiKey);
            if (clientId == null) {
                if (verboseLogging) {
                    System.out.println(RED + "  [AUTH RESULT] FAILED: API key not found in MySQL or active store" + RESET);
                    System.out.println(RED + "  [GATEWAY ACTION] Request BLOCKED with HTTP 401 Unauthorized" + RESET);
                }
                sendError(exchange, 401, "UNAUTHORIZED", "Invalid or revoked API key", null, 0, 0);
                return;
            }

            if (verboseLogging) {
                System.out.println(GREEN + "  [AUTH RESULT] SUCCESS: Client authenticated -> " + BOLD + clientId + RESET);
            }

            // Step 2: Rate Limit Policy Lookup & Evaluation
            if (verboseLogging) {
                System.out.println();
                System.out.println(CYAN + "  [STEP 2: RATE LIMIT POLICY EVALUATION]" + RESET);
                System.out.println("  Looking up active policy for client: " + clientId + "...");
            }

            RateLimitPolicy policy = rateLimiterService.getPolicy(clientId);
            if (policy == null) {
                if (verboseLogging) {
                    System.out.println(RED + "  [POLICY ERROR] No active rate-limiting policy found for client" + RESET);
                }
                sendError(exchange, 400, "BAD_REQUEST", "Invalid or missing rate limit policy", null, 0, 0);
                return;
            }

            if (verboseLogging) {
                System.out.println("  Policy Name  : " + policy.getPolicyId());
                System.out.println("  Algorithm    : " + BOLD + policy.getAlgorithmType() + RESET);
                if (policy.getAlgorithmType() == AlgorithmType.TOKEN_BUCKET) {
                    System.out.println("  Capacity     : " + policy.getLimit() + " tokens (Refill: "
                            + policy.getRefillTokens() + " tokens / " + policy.getRefillDurationMillis() + " ms)");
                } else {
                    System.out.println("  Limit        : " + policy.getLimit() + " requests per "
                            + policy.getWindowDurationMillis() + " ms");
                }
            }

            long currentTimeMillis = System.currentTimeMillis();
            RateLimitResult result = rateLimiterService.evaluate(clientId, currentTimeMillis);

            if (result == null) {
                sendError(exchange, 500, "ERROR", "Rate limit evaluation failed internally", null, 0, 0);
                return;
            }

            long remaining = (long) Math.max(0, Math.floor(result.getRemainingCapacity()));

            // Step 3: Gateway Decision & Routing
            if (verboseLogging) {
                System.out.println();
                System.out.println(CYAN + "  [STEP 3: GATEWAY DECISION & FORWARDING]" + RESET);
            }

            if (!result.isAllowed()) {
                Long retryAfterMs = result.getRetryAfterMillis();
                long retrySeconds = (retryAfterMs != null && retryAfterMs > 0)
                        ? (long) Math.ceil(retryAfterMs / 1000.0)
                        : 1;

                if (verboseLogging) {
                    System.out.println(RED + BOLD + "  [DECISION] REJECTED (HTTP 429 Too Many Requests)" + RESET);
                    System.out.println(RED + "  Reason      : " + result.getMessage() + RESET);
                    System.out.println(YELLOW + "  Remaining   : 0 tokens / requests" + RESET);
                    System.out.println(YELLOW + "  Retry-After : " + retryAfterMs + " ms (" + retrySeconds + " s)" + RESET);
                    System.out.println(RED + "  [GATEWAY ACTION] Request INTERCEPTED & BLOCKED. Upstream NOT called." + RESET);
                }

                sendError(exchange, 429, "REJECTED", result.getMessage(), clientId, remaining, retrySeconds);
                printRequestCompletedFooter(429, startTime);
                return;
            }

            if (verboseLogging) {
                System.out.println(GREEN + BOLD + "  [DECISION] ALLOWED (HTTP 200 OK)" + RESET);
                System.out.println(GREEN + "  Remaining   : " + remaining + " tokens / requests" + RESET);
                System.out.println(CYAN + "  [GATEWAY ACTION] Forwarding request to upstream service for " + path + "..." + RESET);
            }

            // Step 4: Upstream Execution & Response Dispatching
            String upstreamData = executeUpstreamMock(path, clientId, requestId);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("X-RateLimit-Limit", String.valueOf(policy.getLimit()));
            exchange.getResponseHeaders().set("X-RateLimit-Remaining", String.valueOf(remaining));

            String responseBody = String.format(
                    "{\"status\":\"ALLOWED\",\"requestId\":\"REQ-%d\",\"clientId\":\"%s\",\"endpoint\":\"%s\",\"algorithm\":\"%s\",\"limit\":%d,\"remainingCapacity\":%d,\"data\":%s}",
                    requestId,
                    clientId,
                    path,
                    policy.getAlgorithmType().name(),
                    policy.getLimit(),
                    remaining,
                    upstreamData
            );

            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }

            if (verboseLogging) {
                System.out.println();
                System.out.println(CYAN + "  [STEP 4: RESPONSE DISPATCH]" + RESET);
                System.out.println(GREEN + "  Status Code : 200 OK" + RESET);
                System.out.println("  Headers     : X-RateLimit-Limit=" + policy.getLimit()
                        + ", X-RateLimit-Remaining=" + remaining);
                printRequestCompletedFooter(200, startTime);
            }
        }

        private void handleHealthCheck(HttpExchange exchange, long requestId, long startTime) throws IOException {
            String json = String.format(
                    "{\"status\":\"UP\",\"application\":\"Throttlix Gateway\",\"requestId\":\"REQ-%d\",\"timestamp\":\"%s\"}",
                    requestId,
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date())
            );
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
            if (verboseLogging) {
                System.out.println(GREEN + "  [HEALTH] Health check request answered (200 OK)" + RESET);
                printRequestCompletedFooter(200, startTime);
            }
        }

        private void sendError(HttpExchange exchange, int statusCode, String status, String message,
                               String clientId, long remaining, long retryAfterSeconds) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            if (retryAfterSeconds > 0) {
                exchange.getResponseHeaders().set("Retry-After", String.valueOf(retryAfterSeconds));
            }
            if (clientId != null) {
                exchange.getResponseHeaders().set("X-RateLimit-Remaining", String.valueOf(remaining));
            }

            String json;
            if (retryAfterSeconds > 0) {
                json = String.format(
                        "{\"status\":\"%s\",\"statusCode\":%d,\"clientId\":\"%s\",\"message\":\"%s\",\"remainingCapacity\":%d,\"retryAfterSeconds\":%d}",
                        status, statusCode, clientId != null ? clientId : "unknown", message, remaining, retryAfterSeconds
                );
            } else {
                json = String.format(
                        "{\"status\":\"%s\",\"statusCode\":%d,\"message\":\"%s\"}",
                        status, statusCode, message
                );
            }

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String executeUpstreamMock(String path, String clientId, long requestId) {
            if (path.contains("products")) {
                return "[{\"id\":101,\"name\":\"Cloud Microservice API\",\"tier\":\"Enterprise\"},{\"id\":102,\"name\":\"Event Stream Processor\",\"tier\":\"Standard\"}]";
            } else if (path.contains("users")) {
                return "[{\"userId\":1,\"username\":\"lead-architect\",\"role\":\"Admin\"},{\"userId\":2,\"username\":\"api-client\",\"role\":\"User\"}]";
            } else if (path.contains("limit")) {
                return "{\"telemetry\":\"Rate limit evaluation successful\",\"timestamp\":" + System.currentTimeMillis() + "}";
            } else {
                return "{\"resource\":\"" + path + "\",\"processedBy\":\"Throttlix Gateway Upstream\"}";
            }
        }

        private void printRequestCompletedFooter(int statusCode, long startTime) {
            long latency = System.currentTimeMillis() - startTime;
            String color = statusCode == 200 ? GREEN : (statusCode == 429 ? YELLOW : RED);
            System.out.println("  Duration    : " + latency + " ms");
            System.out.println(color + "  [COMPLETED] Handled request with HTTP " + statusCode + RESET);
            System.out.println(CYAN + "--------------------------------------------------------------------------------" + RESET);
            System.out.println();
        }
    }

    private static String maskApiKey(String key) {
        if (key == null || key.isBlank()) {
            return "[MISSING]";
        }
        String clean = key.trim();
        if (clean.length() <= 6) {
            return "***";
        }
        return clean.substring(0, 4) + "****" + clean.substring(clean.length() - 2);
    }
}
