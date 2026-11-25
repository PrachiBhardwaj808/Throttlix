package com.throttlix.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GatewayServerIntegrationTest {

    private static GatewayServer server;
    private static int port;
    private static HttpClient client;

    @BeforeAll
    static void setUp() throws Exception {
        // Port 0 tells the OS to assign any free ephemeral port
        server = new GatewayServer(0);
        server.start();
        port = server.getPort();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Public Health Check returns 200 OK without requiring API key")
    void shouldReturnHealthCheckSuccessfully() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
        assertTrue(response.body().contains("Throttlix Gateway"));
    }

    @Test
    @DisplayName("Request with valid API Key returns 200 OK and decrements capacity")
    void shouldAllowValidRequestAndSetRateLimitHeaders() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("X-API-Key", "thx-test-key-1")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"ALLOWED\""));
        assertTrue(response.body().contains("client-analytics"));

        String limitHeader = response.headers().firstValue("X-RateLimit-Limit").orElse(null);
        String remainingHeader = response.headers().firstValue("X-RateLimit-Remaining").orElse(null);

        assertNotNull(limitHeader);
        assertNotNull(remainingHeader);
        assertEquals("10", limitHeader);
    }

    @Test
    @DisplayName("Missing API Key returns HTTP 401 Unauthorized")
    void shouldRejectMissingApiKeyWith401() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UNAUTHORIZED\""));
        assertTrue(response.body().contains("Missing X-API-Key header"));
    }

    @Test
    @DisplayName("Invalid or unrecognized API Key returns HTTP 401 Unauthorized")
    void shouldRejectInvalidApiKeyWith401() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/products"))
                .header("X-API-Key", "unrecognized-bogus-key-12345")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UNAUTHORIZED\""));
        assertTrue(response.body().contains("Invalid or revoked API key"));
    }

    @Test
    @DisplayName("Sliding Window key routes and evaluates sliding window policy")
    void shouldEvaluateSlidingWindowClient() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/users"))
                .header("X-API-Key", "thx-test-key-2")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"ALLOWED\""));
        assertTrue(response.body().contains("client-mobile"));
        assertTrue(response.body().contains("SLIDING_WINDOW"));
        assertEquals("5", response.headers().firstValue("X-RateLimit-Limit").orElse(null));
    }

    @Test
    @DisplayName("Burst of requests exceeding capacity returns HTTP 429 Too Many Requests")
    void shouldReturn429WhenLimitExceeded() throws Exception {
        // Send a burst of 15 rapid requests for client-analytics (capacity 10)
        HttpResponse<String> lastResponse = null;
        boolean saw429 = false;

        for (int i = 0; i < 15; i++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/products"))
                    .header("X-API-Key", "thx-test-key-1")
                    .GET()
                    .build();

            lastResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (lastResponse.statusCode() == 429) {
                saw429 = true;
                break;
            }
        }

        assertTrue(saw429, "Expected to encounter HTTP 429 Too Many Requests during high-volume burst");
        assertNotNull(lastResponse);
        assertEquals(429, lastResponse.statusCode());
        assertTrue(lastResponse.body().contains("\"status\":\"REJECTED\""));
        assertTrue(lastResponse.headers().firstValue("Retry-After").isPresent());
    }
}
