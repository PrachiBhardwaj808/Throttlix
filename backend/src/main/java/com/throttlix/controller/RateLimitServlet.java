package com.throttlix.controller;

import com.throttlix.auth.ApiKeyAuthService;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.core.RateLimitResult;
import com.throttlix.service.RateLimiterService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/limit")
public class RateLimitServlet extends HttpServlet {

    private final ApiKeyAuthService authService;
    private final RateLimiterService rateLimiterService;

    public RateLimitServlet() {
        this(new ApiKeyAuthService(), new RateLimiterService());
    }

    public RateLimitServlet(ApiKeyAuthService authService, RateLimiterService rateLimiterService) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            String method = request.getMethod();
            String path = request.getRequestURI();
            String apiKey = request.getHeader("X-API-Key");

            if (apiKey == null || apiKey.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("""
                        {"status":"UNAUTHORIZED","message":"Missing X-API-Key header"}""");
                return;
            }

            String clientId = authService.authenticate(apiKey);
            if (clientId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("""
                        {"status":"UNAUTHORIZED","message":"Invalid API key"}""");
                return;
            }

            long currentTimeMillis = System.currentTimeMillis();
            RateLimitPolicy policy = rateLimiterService.getPolicy(clientId);
            RateLimitResult result = rateLimiterService.evaluate(clientId, currentTimeMillis);

            response.setHeader("X-RateLimit-Limit", String.valueOf(policy.getLimit()));
            long remaining = (long) Math.max(0, Math.floor(result.getRemainingCapacity()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

            if (result.isAllowed()) {
                response.setStatus(HttpServletResponse.SC_OK);
                String json = String.format(
                        "{\"status\":\"ALLOWED\",\"clientId\":\"%s\",\"algorithm\":\"%s\",\"limit\":%d,\"remainingCapacity\":%d,\"message\":\"%s\"}",
                        clientId,
                        policy.getAlgorithmType().name(),
                        policy.getLimit(),
                        remaining,
                        result.getMessage()
                );
                response.getWriter().write(json);
            } else {
                response.setStatus(429);
                Long retryAfterMillis = result.getRetryAfterMillis();
                long retryAfterSeconds = (retryAfterMillis != null && retryAfterMillis > 0)
                        ? (long) Math.ceil(retryAfterMillis / 1000.0)
                        : 1;
                response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
                long retryMs = (retryAfterMillis != null) ? retryAfterMillis : 0L;
                String json = String.format(
                        "{\"status\":\"REJECTED\",\"clientId\":\"%s\",\"algorithm\":\"%s\",\"limit\":%d,\"remainingCapacity\":0,\"retryAfterMs\":%d,\"retryAfterSeconds\":%d,\"message\":\"%s\"}",
                        clientId,
                        policy.getAlgorithmType().name(),
                        policy.getLimit(),
                        retryMs,
                        retryAfterSeconds,
                        result.getMessage()
                );
                response.getWriter().write(json);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("""
                    {"status":"ERROR","message":"An unexpected error occurred while processing the request"}""");
        }
    }
}
