package com.throttlix.controller;

import com.throttlix.auth.ApiKeyAuthService;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.core.RateLimitResult;
import com.throttlix.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitServletTest {

    @Mock
    private ApiKeyAuthService authService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private RateLimitServlet servlet;
    private StringWriter responseBodyWriter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        servlet = new RateLimitServlet(authService, rateLimiterService);
        responseBodyWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBodyWriter));
    }

    @Test
    void shouldReturn401WhenApiKeyHeaderIsMissing() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String output = responseBodyWriter.toString();
        assertTrue(output.contains("UNAUTHORIZED"));
        assertTrue(output.contains("Missing X-API-Key header"));
    }

    @Test
    void shouldReturn401WhenApiKeyHeaderIsEmptyOrWhitespace() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn("   ");

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String output = responseBodyWriter.toString();
        assertTrue(output.contains("UNAUTHORIZED"));
        assertTrue(output.contains("Missing X-API-Key header"));
    }

    @Test
    void shouldReturn401WhenApiKeyIsInvalid() throws Exception {
        String invalidKey = "invalid-secret-key-xyz";
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn(invalidKey);
        when(authService.authenticate(invalidKey)).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String output = responseBodyWriter.toString();
        assertTrue(output.contains("UNAUTHORIZED"));
        assertTrue(output.contains("Invalid API key"));
        assertFalse(output.contains(invalidKey));
    }

    @Test
    void shouldReturn400WhenApiKeyHeaderContainsMalformedControlCharacters() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn("key\nwith\rnewline");

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String output = responseBodyWriter.toString();
        assertTrue(output.contains("BAD_REQUEST"));
        assertTrue(output.contains("Malformed X-API-Key header format"));
    }

    @Test
    void shouldReturn400WhenTokensParameterIsNotAnInteger() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn("thx-test-key-1");
        when(request.getParameter("tokens")).thenReturn("not-a-number");

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String output = responseBodyWriter.toString();
        assertTrue(output.contains("BAD_REQUEST"));
        assertTrue(output.contains("Malformed tokens parameter"));
    }

    @Test
    void shouldReturn400WhenTokensParameterIsZeroOrNegative() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn("thx-test-key-1");
        when(request.getParameter("tokens")).thenReturn("-3");

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String output = responseBodyWriter.toString();
        assertTrue(output.contains("BAD_REQUEST"));
        assertTrue(output.contains("must be a positive integer"));
    }

    @Test
    void shouldReturn400WhenAlgorithmParameterIsUnsupported() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn("thx-test-key-1");
        when(request.getParameter("algorithm")).thenReturn("LEAKY_BUCKET_UNSUPPORTED");

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String output = responseBodyWriter.toString();
        assertTrue(output.contains("BAD_REQUEST"));
        assertTrue(output.contains("unsupported algorithm"));
    }

    @Test
    void shouldReturn200AndAccurateHeadersWhenRequestIsAccepted() throws Exception {
        String rawKey = "thx-test-key-1";
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn(rawKey);
        when(authService.authenticate(rawKey)).thenReturn("client-analytics");

        RateLimitPolicy policy = RateLimitPolicy.createTokenBucketPolicy(
                "pol-analytics", "client-analytics", 10, 5, 1000
        );
        when(rateLimiterService.getPolicy("client-analytics")).thenReturn(policy);
        when(rateLimiterService.evaluate(eq("client-analytics"), anyLong()))
                .thenReturn(RateLimitResult.allow(9.0, "Request allowed by Token Bucket"));

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setHeader("X-RateLimit-Limit", "10");
        verify(response).setHeader("X-RateLimit-Remaining", "9");

        String output = responseBodyWriter.toString();
        assertTrue(output.contains("ALLOWED"));
        assertTrue(output.contains("client-analytics"));
        assertTrue(output.contains("TOKEN_BUCKET"));
        assertTrue(output.contains("\"limit\":10"));
        assertTrue(output.contains("\"remainingCapacity\":9"));
        assertFalse(output.contains(rawKey));
    }

    @Test
    void shouldReturn429WithCeilingRoundedRetryAfterWhenRateLimited() throws Exception {
        String rawKey = "thx-test-key-1";
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn(rawKey);
        when(authService.authenticate(rawKey)).thenReturn("client-analytics");

        RateLimitPolicy policy = RateLimitPolicy.createTokenBucketPolicy(
                "pol-analytics", "client-analytics", 10, 5, 1000
        );
        when(rateLimiterService.getPolicy("client-analytics")).thenReturn(policy);
        when(rateLimiterService.evaluate(eq("client-analytics"), anyLong()))
                .thenReturn(RateLimitResult.reject(0.0, 1400L, "Rate limit exceeded"));

        servlet.doPost(request, response);

        verify(response).setStatus(429);
        verify(response).setHeader("X-RateLimit-Limit", "10");
        verify(response).setHeader("X-RateLimit-Remaining", "0");
        verify(response).setHeader("Retry-After", "2");

        String output = responseBodyWriter.toString();
        assertTrue(output.contains("REJECTED"));
        assertTrue(output.contains("\"retryAfterMs\":1400"));
        assertTrue(output.contains("\"retryAfterSeconds\":2"));
        assertTrue(output.contains("\"remainingCapacity\":0"));
        assertFalse(output.contains(rawKey));
    }

    @Test
    void shouldNeverReturnNegativeRetryAfterWhenRateLimited() throws Exception {
        String rawKey = "thx-test-key-1";
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn(rawKey);
        when(authService.authenticate(rawKey)).thenReturn("client-analytics");

        RateLimitPolicy policy = RateLimitPolicy.createTokenBucketPolicy(
                "pol-analytics", "client-analytics", 10, 5, 1000
        );
        when(rateLimiterService.getPolicy("client-analytics")).thenReturn(policy);
        when(rateLimiterService.evaluate(eq("client-analytics"), anyLong()))
                .thenReturn(RateLimitResult.reject(0.0, -100L, "Rate limit exceeded"));

        servlet.doGet(request, response);

        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "1");

        String output = responseBodyWriter.toString();
        assertTrue(output.contains("\"retryAfterMs\":0"));
        assertTrue(output.contains("\"retryAfterSeconds\":1"));
    }

    @Test
    void shouldReturn500WithoutLeakingExceptionDetailsWhenUnexpectedExceptionOccurs() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenThrow(new RuntimeException("Sensitive database connection details"));

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String output = responseBodyWriter.toString();
        assertTrue(output.contains("ERROR"));
        assertFalse(output.contains("Sensitive database connection details"));
    }

    @Test
    void shouldExecuteFullRealPipelineForValidMobileKeyWithSlidingWindow() throws Exception {
        ApiKeyAuthService realAuthService = new ApiKeyAuthService();
        RateLimiterService realRateLimiterService = new RateLimiterService();
        RateLimitServlet realServlet = new RateLimitServlet(realAuthService, realRateLimiterService);

        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn("thx-test-key-2");

        realServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setHeader("X-RateLimit-Limit", "5");
        String output = responseBodyWriter.toString();
        assertTrue(output.contains("ALLOWED"));
        assertTrue(output.contains("client-mobile"));
        assertTrue(output.contains("SLIDING_WINDOW"));
        assertFalse(output.contains("thx-test-key-2"));
    }
}
