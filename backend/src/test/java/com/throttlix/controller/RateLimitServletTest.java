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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void shouldReturn401WhenApiKeyIsInvalid() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn("invalid-key-xyz");
        when(authService.authenticate("invalid-key-xyz")).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String output = responseBodyWriter.toString();
        assertTrue(output.contains("UNAUTHORIZED"));
        assertTrue(output.contains("Invalid API key"));
    }

    @Test
    void shouldReturn200AndAllowedHeadersWhenLimitPermitsRequest() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn("thx-test-key-1");
        when(authService.authenticate("thx-test-key-1")).thenReturn("client-analytics");

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
    }

    @Test
    void shouldReturn429AndRetryAfterWhenLimitIsExceeded() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenReturn("thx-test-key-1");
        when(authService.authenticate("thx-test-key-1")).thenReturn("client-analytics");

        RateLimitPolicy policy = RateLimitPolicy.createTokenBucketPolicy(
                "pol-analytics", "client-analytics", 10, 5, 1000
        );
        when(rateLimiterService.getPolicy("client-analytics")).thenReturn(policy);
        when(rateLimiterService.evaluate(eq("client-analytics"), anyLong()))
                .thenReturn(RateLimitResult.reject(0.0, 1500L, "Rate limit exceeded"));

        servlet.doPost(request, response);

        verify(response).setStatus(429);
        verify(response).setHeader("X-RateLimit-Limit", "10");
        verify(response).setHeader("X-RateLimit-Remaining", "0");
        verify(response).setHeader("Retry-After", "2");

        String output = responseBodyWriter.toString();
        assertTrue(output.contains("REJECTED"));
        assertTrue(output.contains("1500"));
        assertTrue(output.contains("client-analytics"));
    }

    @Test
    void shouldReturn500WhenUnexpectedExceptionOccurs() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/limit");
        when(request.getHeader("X-API-Key")).thenThrow(new RuntimeException("Database timeout simulation"));

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String output = responseBodyWriter.toString();
        assertTrue(output.contains("ERROR"));
        assertTrue(!output.contains("Database timeout simulation"));
    }
}
