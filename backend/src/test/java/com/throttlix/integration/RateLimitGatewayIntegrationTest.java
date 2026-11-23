package com.throttlix.integration;

import com.throttlix.auth.ApiKeyAuthService;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.service.RateLimiterService;
import com.throttlix.controller.RateLimitServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitGatewayIntegrationTest {

    private ApiKeyAuthService authService;
    private RateLimiterService rateLimiterService;
    private RateLimitServlet servlet;

    @BeforeEach
    void setUp() {
        authService = new ApiKeyAuthService();
        rateLimiterService = new RateLimiterService();
        servlet = new RateLimitServlet(authService, rateLimiterService);
    }

    private static class ServletTestResponse {
        final int statusCode;
        final Map<String, String> headers;
        final String body;

        ServletTestResponse(int statusCode, Map<String, String> headers, String body) {
            this.statusCode = statusCode;
            this.headers = headers;
            this.body = body;
        }
    }

    private ServletTestResponse executeRequest(String method, String apiKey, Map<String, String> queryParams)
            throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter responseBodyWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBodyWriter));

        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);

        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                when(request.getParameter(entry.getKey())).thenReturn(entry.getValue());
            }
        }

        Map<String, String> headers = new HashMap<>();
        doAnswer(invocation -> {
            String name = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            headers.put(name, value);
            return null;
        }).when(response).setHeader(anyString(), anyString());

        AtomicInteger statusCode = new AtomicInteger(HttpServletResponse.SC_OK);
        doAnswer(invocation -> {
            int code = invocation.getArgument(0);
            statusCode.set(code);
            return null;
        }).when(response).setStatus(anyInt());

        servlet.service(request, response);

        return new ServletTestResponse(statusCode.get(), headers, responseBodyWriter.toString());
    }

    @Test
    @DisplayName("Scenario 1: Valid API key with Token Bucket policy")
    void testScenario01_validApiKeyWithTokenBucketPolicy() throws Exception {
        ServletTestResponse resp = executeRequest("GET", "thx-test-key-1", null);

        assertEquals(200, resp.statusCode);
        assertEquals("10", resp.headers.get("X-RateLimit-Limit"));
        assertNotNull(resp.headers.get("X-RateLimit-Remaining"));
        assertTrue(resp.body.contains("\"status\":\"ALLOWED\""));
        assertTrue(resp.body.contains("\"algorithm\":\"TOKEN_BUCKET\""));
        assertTrue(resp.body.contains("\"clientId\":\"client-analytics\""));
        assertFalse(resp.body.contains("thx-test-key-1"));
    }

    @Test
    @DisplayName("Scenario 2: Valid API key with Sliding Window policy")
    void testScenario02_validApiKeyWithSlidingWindowPolicy() throws Exception {
        ServletTestResponse resp = executeRequest("GET", "thx-test-key-2", null);

        assertEquals(200, resp.statusCode);
        assertEquals("5", resp.headers.get("X-RateLimit-Limit"));
        assertNotNull(resp.headers.get("X-RateLimit-Remaining"));
        assertTrue(resp.body.contains("\"status\":\"ALLOWED\""));
        assertTrue(resp.body.contains("\"algorithm\":\"SLIDING_WINDOW\""));
        assertTrue(resp.body.contains("\"clientId\":\"client-mobile\""));
        assertFalse(resp.body.contains("thx-test-key-2"));
    }

    @Test
    @DisplayName("Scenario 3: Missing API key")
    void testScenario03_missingApiKey() throws Exception {
        ServletTestResponse respNull = executeRequest("GET", null, null);
        assertEquals(401, respNull.statusCode);
        assertTrue(respNull.body.contains("\"status\":\"UNAUTHORIZED\""));
        assertTrue(respNull.body.contains("Missing X-API-Key header"));

        ServletTestResponse respEmpty = executeRequest("GET", "   ", null);
        assertEquals(401, respEmpty.statusCode);
        assertTrue(respEmpty.body.contains("\"status\":\"UNAUTHORIZED\""));
        assertTrue(respEmpty.body.contains("Missing X-API-Key header"));
    }

    @Test
    @DisplayName("Scenario 4: Invalid API key")
    void testScenario04_invalidApiKey() throws Exception {
        String invalidKey = "completely-unknown-token-999";
        ServletTestResponse resp = executeRequest("GET", invalidKey, null);

        assertEquals(401, resp.statusCode);
        assertTrue(resp.body.contains("\"status\":\"UNAUTHORIZED\""));
        assertTrue(resp.body.contains("Invalid API key"));
        assertFalse(resp.body.contains(invalidKey));
    }

    @Test
    @DisplayName("Scenario 5: Requests within the configured limit")
    void testScenario05_requestsWithinConfiguredLimit() throws Exception {
        authService.registerKey("test-within-limit-key", "client-within-limit");
        rateLimiterService.registerPolicy(RateLimitPolicy.createTokenBucketPolicy(
                "pol-within", "client-within-limit", 5, 1, 60000
        ));

        for (int i = 0; i < 4; i++) {
            ServletTestResponse resp = executeRequest("GET", "test-within-limit-key", null);
            assertEquals(200, resp.statusCode, "Request " + (i + 1) + " should be accepted");
            assertEquals("5", resp.headers.get("X-RateLimit-Limit"));
            assertTrue(resp.body.contains("\"status\":\"ALLOWED\""));
        }
    }

    @Test
    @DisplayName("Scenario 6: Requests exceeding the configured limit")
    void testScenario06_requestsExceedingConfiguredLimit() throws Exception {
        authService.registerKey("test-burst-key", "client-burst");
        rateLimiterService.registerPolicy(RateLimitPolicy.createSlidingWindowPolicy(
                "pol-burst", "client-burst", 3, 60000
        ));

        for (int i = 0; i < 3; i++) {
            ServletTestResponse okResp = executeRequest("POST", "test-burst-key", null);
            assertEquals(200, okResp.statusCode);
        }

        ServletTestResponse rejectResp = executeRequest("POST", "test-burst-key", null);
        assertEquals(429, rejectResp.statusCode);
        assertTrue(rejectResp.body.contains("\"status\":\"REJECTED\""));
        assertEquals("3", rejectResp.headers.get("X-RateLimit-Limit"));
        assertEquals("0", rejectResp.headers.get("X-RateLimit-Remaining"));
    }

    @Test
    @DisplayName("Scenario 7: HTTP 429 response and Retry-After header")
    void testScenario07_http429ResponseAndRetryAfterHeader() throws Exception {
        authService.registerKey("test-retry-key", "client-retry");
        rateLimiterService.registerPolicy(RateLimitPolicy.createTokenBucketPolicy(
                "pol-retry", "client-retry", 1, 1, 5000
        ));

        ServletTestResponse first = executeRequest("GET", "test-retry-key", null);
        assertEquals(200, first.statusCode);

        ServletTestResponse second = executeRequest("GET", "test-retry-key", null);
        assertEquals(429, second.statusCode);
        assertNotNull(second.headers.get("Retry-After"));
        int retrySec = Integer.parseInt(second.headers.get("Retry-After"));
        assertTrue(retrySec >= 1, "Retry-After must be positive");
        assertTrue(second.body.contains("\"retryAfterSeconds\""));
        assertTrue(second.body.contains("\"retryAfterMs\""));
    }

    @Test
    @DisplayName("Scenario 8: X-RateLimit-Limit and X-RateLimit-Remaining headers")
    void testScenario08_rateLimitHeadersAccuracy() throws Exception {
        authService.registerKey("test-headers-key", "client-headers");
        rateLimiterService.registerPolicy(RateLimitPolicy.createTokenBucketPolicy(
                "pol-headers", "client-headers", 4, 1, 60000
        ));

        ServletTestResponse r1 = executeRequest("GET", "test-headers-key", null);
        assertEquals("4", r1.headers.get("X-RateLimit-Limit"));
        assertEquals("3", r1.headers.get("X-RateLimit-Remaining"));

        ServletTestResponse r2 = executeRequest("GET", "test-headers-key", null);
        assertEquals("4", r2.headers.get("X-RateLimit-Limit"));
        assertEquals("2", r2.headers.get("X-RateLimit-Remaining"));
    }

    @Test
    @DisplayName("Scenario 9: Independent clients with separate rate-limit states")
    void testScenario09_independentClientsWithSeparateStates() throws Exception {
        authService.registerKey("key-client-a", "client-isolated-a");
        authService.registerKey("key-client-b", "client-isolated-b");

        rateLimiterService.registerPolicy(RateLimitPolicy.createSlidingWindowPolicy(
                "pol-iso-a", "client-isolated-a", 2, 60000
        ));
        rateLimiterService.registerPolicy(RateLimitPolicy.createTokenBucketPolicy(
                "pol-iso-b", "client-isolated-b", 5, 2, 60000
        ));

        executeRequest("GET", "key-client-a", null);
        executeRequest("GET", "key-client-a", null);
        ServletTestResponse aRejected = executeRequest("GET", "key-client-a", null);
        assertEquals(429, aRejected.statusCode, "Client A should now be rate-limited");

        ServletTestResponse bAccepted = executeRequest("GET", "key-client-b", null);
        assertEquals(200, bAccepted.statusCode, "Client B must not be affected by Client A exhaustion");
        assertEquals("5", bAccepted.headers.get("X-RateLimit-Limit"));
        assertEquals("4", bAccepted.headers.get("X-RateLimit-Remaining"));
    }

    @Test
    @DisplayName("Scenario 10: Invalid or missing policy")
    void testScenario10_invalidOrMissingPolicy() throws Exception {
        RateLimiterService nullPolicyService = mock(RateLimiterService.class);
        when(nullPolicyService.getPolicy("client-analytics")).thenReturn(null);
        RateLimitServlet servletWithNullPolicy = new RateLimitServlet(authService, nullPolicyService);

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        when(res.getWriter()).thenReturn(new PrintWriter(writer));
        when(req.getMethod()).thenReturn("GET");
        when(req.getRequestURI()).thenReturn("/api/limit");
        when(req.getHeader("X-API-Key")).thenReturn("thx-test-key-1");

        servletWithNullPolicy.service(req, res);

        verify(res).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(writer.toString().contains("BAD_REQUEST"));
        assertTrue(writer.toString().contains("Invalid or missing rate limit policy"));
    }

    @Test
    @DisplayName("Scenario 11: Unsupported HTTP method")
    void testScenario11_unsupportedHttpMethod() throws Exception {
        ServletTestResponse putResp = executeRequest("PUT", "thx-test-key-1", null);
        assertEquals(405, putResp.statusCode);
        assertEquals("GET, POST", putResp.headers.get("Allow"));
        assertTrue(putResp.body.contains("METHOD_NOT_ALLOWED"));

        ServletTestResponse deleteResp = executeRequest("DELETE", "thx-test-key-1", null);
        assertEquals(405, deleteResp.statusCode);
        assertEquals("GET, POST", deleteResp.headers.get("Allow"));
        assertTrue(deleteResp.body.contains("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("Scenario 12: Repeated requests verify state persists between requests")
    void testScenario12_repeatedRequestsStatePersistence() throws Exception {
        authService.registerKey("key-persistent", "client-persistent");
        rateLimiterService.registerPolicy(RateLimitPolicy.createTokenBucketPolicy(
                "pol-persist", "client-persistent", 3, 1, 60000
        ));

        ServletTestResponse r1 = executeRequest("GET", "key-persistent", null);
        assertEquals(200, r1.statusCode);
        assertEquals("2", r1.headers.get("X-RateLimit-Remaining"));

        ServletTestResponse r2 = executeRequest("GET", "key-persistent", null);
        assertEquals(200, r2.statusCode);
        assertEquals("1", r2.headers.get("X-RateLimit-Remaining"));

        ServletTestResponse r3 = executeRequest("GET", "key-persistent", null);
        assertEquals(200, r3.statusCode);
        assertEquals("0", r3.headers.get("X-RateLimit-Remaining"));

        ServletTestResponse r4 = executeRequest("GET", "key-persistent", null);
        assertEquals(429, r4.statusCode);
    }
}
