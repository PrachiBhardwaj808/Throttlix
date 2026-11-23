package com.throttlix.controller;

import com.throttlix.auth.ApiKeyAuthService;
import com.throttlix.core.AlgorithmType;
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
            String apiKey = request.getHeader("X-API-Key");

            if (apiKey == null || apiKey.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("""
                        {"status":"UNAUTHORIZED","message":"Missing X-API-Key header"}""");
                return;
            }

            if (hasMalformedHeaderChars(apiKey)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("""
                        {"status":"BAD_REQUEST","message":"Malformed X-API-Key header format"}""");
                return;
            }

            String tokensParam = request.getParameter("tokens");
            if (tokensParam != null) {
                try {
                    int requestedTokens = Integer.parseInt(tokensParam.trim());
                    if (requestedTokens <= 0) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().write("""
                                {"status":"BAD_REQUEST","message":"Invalid tokens parameter: must be a positive integer"}""");
                        return;
                    }
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("""
                            {"status":"BAD_REQUEST","message":"Malformed tokens parameter: must be an integer"}""");
                    return;
                }
            }

            String algoParam = request.getParameter("algorithm");
            if (algoParam != null && !algoParam.trim().isEmpty()) {
                try {
                    AlgorithmType.valueOf(algoParam.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("""
                            {"status":"BAD_REQUEST","message":"Malformed algorithm parameter: unsupported algorithm"}""");
                    return;
                }
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
                long retryAfterSeconds = calculateRetryAfterSeconds(retryAfterMillis);
                response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

                long retryMs = (retryAfterMillis != null && retryAfterMillis > 0) ? retryAfterMillis : 0L;
                String json = String.format(
                        "{\"status\":\"REJECTED\",\"clientId\":\"%s\",\"algorithm\":\"%s\",\"limit\":%d,\"remainingCapacity\":%d,\"retryAfterMs\":%d,\"retryAfterSeconds\":%d,\"message\":\"%s\"}",
                        clientId,
                        policy.getAlgorithmType().name(),
                        policy.getLimit(),
                        remaining,
                        retryMs,
                        retryAfterSeconds,
                        result.getMessage()
                );
                response.getWriter().write(json);
            }
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("""
                    {"status":"BAD_REQUEST","message":"Malformed request syntax or invalid parameters"}""");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("""
                    {"status":"ERROR","message":"An unexpected error occurred while processing the request"}""");
        }
    }

    private boolean hasMalformedHeaderChars(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 32 || c == 127) {
                return true;
            }
        }
        return false;
    }

    private long calculateRetryAfterSeconds(Long retryAfterMillis) {
        if (retryAfterMillis == null || retryAfterMillis <= 0) {
            return 1;
        }
        return (long) Math.ceil(retryAfterMillis / 1000.0);
    }
}
