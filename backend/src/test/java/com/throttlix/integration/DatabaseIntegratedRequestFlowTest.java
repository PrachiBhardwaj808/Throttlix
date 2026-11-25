package com.throttlix.integration;

import com.throttlix.auth.ApiKeyAuthService;
import com.throttlix.controller.RateLimitServlet;
import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.dao.ApiKeyDAO;
import com.throttlix.dao.RateLimitPolicyDAO;
import com.throttlix.database.DatabaseConfig;
import com.throttlix.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseIntegratedRequestFlowTest {

    private static class ServletResponseHolder {
        final int statusCode;
        final Map<String, String> headers;
        final String body;

        ServletResponseHolder(int statusCode, Map<String, String> headers, String body) {
            this.statusCode = statusCode;
            this.headers = headers;
            this.body = body;
        }
    }

    private ServletResponseHolder executeServlet(RateLimitServlet servlet, String method, String apiKey) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter bodyWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(bodyWriter));

        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);

        Map<String, String> headers = new HashMap<>();
        doAnswer(inv -> {
            headers.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(response).setHeader(anyString(), anyString());

        AtomicInteger status = new AtomicInteger(HttpServletResponse.SC_OK);
        doAnswer(inv -> {
            status.set(inv.getArgument(0));
            return null;
        }).when(response).setStatus(anyInt());

        servlet.service(request, response);
        return new ServletResponseHolder(status.get(), headers, bodyWriter.toString());
    }

    @Test
    @DisplayName("Complete Flow: Valid API Key in MySQL with Token Bucket policy executes successfully")
    void testCompleteFlow_validApiKeyAndTokenBucketPolicyFromMySQL() throws Exception {
        ApiKeyDAO apiKeyDAO = mock(ApiKeyDAO.class);
        RateLimitPolicyDAO policyDAO = mock(RateLimitPolicyDAO.class);
        DatabaseConfig dbConfig = mock(DatabaseConfig.class);
        Connection connection = mock(Connection.class);

        when(dbConfig.getConnection()).thenReturn(connection);

        String rawKey = "live-prod-secret-key-1";
        String keyHash = ApiKeyAuthService.hashKey(rawKey);

        when(apiKeyDAO.findClientByActiveKeyHash(connection, keyHash))
                .thenReturn(Optional.of("client-ecommerce"));

        RateLimitPolicy dbPolicy = RateLimitPolicy.createTokenBucketPolicy(
                "pol-tb-ecommerce", "client-ecommerce", 5, 2, 60_000L
        );
        when(policyDAO.findActivePolicy(connection, "client-ecommerce"))
                .thenReturn(Optional.of(dbPolicy));

        ApiKeyAuthService authService = new ApiKeyAuthService(apiKeyDAO, dbConfig);
        RateLimiterService rateLimiterService = new RateLimiterService(
                new com.throttlix.algorithm.tokenbucket.TokenBucketRateLimiter(),
                new com.throttlix.algorithm.slidingwindow.SlidingWindow(),
                policyDAO,
                dbConfig
        );
        RateLimitServlet servlet = new RateLimitServlet(authService, rateLimiterService);

        ServletResponseHolder response = executeServlet(servlet, "GET", rawKey);

        assertEquals(200, response.statusCode);
        assertEquals("5", response.headers.get("X-RateLimit-Limit"));
        assertEquals("4", response.headers.get("X-RateLimit-Remaining"));
        assertTrue(response.body.contains("\"status\":\"ALLOWED\""));
        assertTrue(response.body.contains("client-ecommerce"));
        assertTrue(response.body.contains("TOKEN_BUCKET"));
        assertFalse(response.body.contains(rawKey));
    }

    @Test
    @DisplayName("Complete Flow: Valid API Key in MySQL with Sliding Window policy rejects when quota exceeded")
    void testCompleteFlow_slidingWindowExceededWithRetryAfter() throws Exception {
        ApiKeyDAO apiKeyDAO = mock(ApiKeyDAO.class);
        RateLimitPolicyDAO policyDAO = mock(RateLimitPolicyDAO.class);
        DatabaseConfig dbConfig = mock(DatabaseConfig.class);
        Connection connection = mock(Connection.class);

        when(dbConfig.getConnection()).thenReturn(connection);

        String rawKey = "live-sw-secret-key-2";
        String keyHash = ApiKeyAuthService.hashKey(rawKey);

        when(apiKeyDAO.findClientByActiveKeyHash(connection, keyHash))
                .thenReturn(Optional.of("client-iot"));

        RateLimitPolicy dbPolicy = RateLimitPolicy.createSlidingWindowPolicy(
                "pol-sw-iot", "client-iot", 1, 60_000L
        );
        when(policyDAO.findActivePolicy(connection, "client-iot"))
                .thenReturn(Optional.of(dbPolicy));

        ApiKeyAuthService authService = new ApiKeyAuthService(apiKeyDAO, dbConfig);
        RateLimiterService rateLimiterService = new RateLimiterService(
                new com.throttlix.algorithm.tokenbucket.TokenBucketRateLimiter(),
                new com.throttlix.algorithm.slidingwindow.SlidingWindow(),
                policyDAO,
                dbConfig
        );
        RateLimitServlet servlet = new RateLimitServlet(authService, rateLimiterService);

        ServletResponseHolder first = executeServlet(servlet, "GET", rawKey);
        assertEquals(200, first.statusCode);

        ServletResponseHolder second = executeServlet(servlet, "GET", rawKey);
        assertEquals(429, second.statusCode);
        assertNotNull(second.headers.get("Retry-After"));
        assertTrue(second.body.contains("\"status\":\"REJECTED\""));
        assertTrue(second.body.contains("client-iot"));
        assertTrue(second.body.contains("SLIDING_WINDOW"));
    }

    @Test
    @DisplayName("Security Flow: Revoked API key in MySQL returns HTTP 401 Unauthorized")
    void testRevokedApiKeyInMySQL_returns401() throws Exception {
        ApiKeyDAO apiKeyDAO = mock(ApiKeyDAO.class);
        RateLimitPolicyDAO policyDAO = mock(RateLimitPolicyDAO.class);
        DatabaseConfig dbConfig = mock(DatabaseConfig.class);
        Connection connection = mock(Connection.class);

        when(dbConfig.getConnection()).thenReturn(connection);

        String rawKey = "revoked-secret-key";
        String keyHash = ApiKeyAuthService.hashKey(rawKey);

        when(apiKeyDAO.findClientByActiveKeyHash(connection, keyHash))
                .thenReturn(Optional.empty());

        ApiKeyAuthService authService = new ApiKeyAuthService(apiKeyDAO, dbConfig);
        RateLimiterService rateLimiterService = new RateLimiterService(
                new com.throttlix.algorithm.tokenbucket.TokenBucketRateLimiter(),
                new com.throttlix.algorithm.slidingwindow.SlidingWindow(),
                policyDAO,
                dbConfig
        );
        RateLimitServlet servlet = new RateLimitServlet(authService, rateLimiterService);

        ServletResponseHolder response = executeServlet(servlet, "GET", rawKey);

        assertEquals(401, response.statusCode);
        assertTrue(response.body.contains("\"status\":\"UNAUTHORIZED\""));
        assertTrue(response.body.contains("Invalid API key"));
        assertFalse(response.body.contains(rawKey));
    }

    @Test
    @DisplayName("Fault Tolerance: Database connection failure during auth returns 401 without leaking internal error")
    void testDatabaseOutageDuringAuth_safe401Failure() throws Exception {
        ApiKeyDAO apiKeyDAO = mock(ApiKeyDAO.class);
        RateLimitPolicyDAO policyDAO = mock(RateLimitPolicyDAO.class);
        DatabaseConfig dbConfig = mock(DatabaseConfig.class);

        when(dbConfig.getConnection()).thenThrow(new SQLException("Communications link failure: Connection refused"));

        String rawKey = "any-key-during-outage";

        ApiKeyAuthService authService = new ApiKeyAuthService(apiKeyDAO, dbConfig);
        RateLimiterService rateLimiterService = new RateLimiterService(
                new com.throttlix.algorithm.tokenbucket.TokenBucketRateLimiter(),
                new com.throttlix.algorithm.slidingwindow.SlidingWindow(),
                policyDAO,
                dbConfig
        );
        RateLimitServlet servlet = new RateLimitServlet(authService, rateLimiterService);

        ServletResponseHolder response = executeServlet(servlet, "GET", rawKey);

        assertEquals(401, response.statusCode);
        assertTrue(response.body.contains("\"status\":\"UNAUTHORIZED\""));
        assertFalse(response.body.contains("Communications link failure"));
    }

    @Test
    @DisplayName("Fault Tolerance: Database failure during policy retrieval returns 400 Bad Request")
    void testDatabaseOutageDuringPolicyRetrieval_safe400Failure() throws Exception {
        ApiKeyDAO apiKeyDAO = mock(ApiKeyDAO.class);
        RateLimitPolicyDAO policyDAO = mock(RateLimitPolicyDAO.class);
        DatabaseConfig dbConfig = mock(DatabaseConfig.class);
        Connection connection = mock(Connection.class);

        when(dbConfig.getConnection()).thenReturn(connection);

        String rawKey = "valid-key-but-policy-fails";
        String keyHash = ApiKeyAuthService.hashKey(rawKey);

        when(apiKeyDAO.findClientByActiveKeyHash(connection, keyHash))
                .thenReturn(Optional.of("client-orphan"));

        when(policyDAO.findActivePolicy(connection, "client-orphan"))
                .thenThrow(new SQLException("Table 'throttlix.rate_limit_policies' doesn't exist"));

        ApiKeyAuthService authService = new ApiKeyAuthService(apiKeyDAO, dbConfig);
        RateLimiterService rateLimiterService = new RateLimiterService(
                new com.throttlix.algorithm.tokenbucket.TokenBucketRateLimiter(),
                new com.throttlix.algorithm.slidingwindow.SlidingWindow(),
                policyDAO,
                dbConfig
        );
        RateLimitServlet servlet = new RateLimitServlet(authService, rateLimiterService);

        ServletResponseHolder response = executeServlet(servlet, "GET", rawKey);

        assertEquals(400, response.statusCode);
        assertTrue(response.body.contains("\"status\":\"BAD_REQUEST\""));
        assertTrue(response.body.contains("Invalid or missing rate limit policy"));
        assertFalse(response.body.contains("Table 'throttlix.rate_limit_policies' doesn't exist"));
    }

    @Test
    @DisplayName("Missing Policy in MySQL returns HTTP 400 Bad Request")
    void testMissingPolicyInMySQL_returns400() throws Exception {
        ApiKeyDAO apiKeyDAO = mock(ApiKeyDAO.class);
        RateLimitPolicyDAO policyDAO = mock(RateLimitPolicyDAO.class);
        DatabaseConfig dbConfig = mock(DatabaseConfig.class);
        Connection connection = mock(Connection.class);

        when(dbConfig.getConnection()).thenReturn(connection);

        String rawKey = "valid-key-no-policy";
        String keyHash = ApiKeyAuthService.hashKey(rawKey);

        when(apiKeyDAO.findClientByActiveKeyHash(connection, keyHash))
                .thenReturn(Optional.of("client-unconfigured"));

        when(policyDAO.findActivePolicy(connection, "client-unconfigured"))
                .thenReturn(Optional.empty());

        ApiKeyAuthService authService = new ApiKeyAuthService(apiKeyDAO, dbConfig);
        RateLimiterService rateLimiterService = new RateLimiterService(
                new com.throttlix.algorithm.tokenbucket.TokenBucketRateLimiter(),
                new com.throttlix.algorithm.slidingwindow.SlidingWindow(),
                policyDAO,
                dbConfig
        );
        RateLimitServlet servlet = new RateLimitServlet(authService, rateLimiterService);

        ServletResponseHolder response = executeServlet(servlet, "GET", rawKey);

        assertEquals(400, response.statusCode);
        assertTrue(response.body.contains("\"status\":\"BAD_REQUEST\""));
        assertTrue(response.body.contains("Invalid or missing rate limit policy"));
    }
}
