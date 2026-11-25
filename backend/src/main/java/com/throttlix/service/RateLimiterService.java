package com.throttlix.service;

import com.throttlix.algorithm.slidingwindow.SlidingWindow;
import com.throttlix.algorithm.tokenbucket.TokenBucketRateLimiter;
import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import com.throttlix.core.RateLimitResult;
import com.throttlix.core.RateLimiter;
import com.throttlix.dao.RateLimitPolicyDAO;
import com.throttlix.database.DatabaseConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RateLimiterService {

    private static final Logger LOGGER = Logger.getLogger(RateLimiterService.class.getName());

    private final RateLimiter tokenBucketLimiter;
    private final RateLimiter slidingWindowLimiter;
    private final RateLimitPolicyDAO policyDAO;
    private final DatabaseConfig databaseConfig;
    private final ConcurrentMap<String, RateLimitPolicy> clientPolicies = new ConcurrentHashMap<>();

    public RateLimiterService() {
        this(new TokenBucketRateLimiter(), new SlidingWindow(), new RateLimitPolicyDAO(), new DatabaseConfig());
    }

    public RateLimiterService(RateLimiter tokenBucketLimiter, RateLimiter slidingWindowLimiter) {
        this(tokenBucketLimiter, slidingWindowLimiter, new RateLimitPolicyDAO(), new DatabaseConfig());
    }

    public RateLimiterService(RateLimiter tokenBucketLimiter, RateLimiter slidingWindowLimiter,
                              RateLimitPolicyDAO policyDAO, DatabaseConfig databaseConfig) {
        this.tokenBucketLimiter = tokenBucketLimiter;
        this.slidingWindowLimiter = slidingWindowLimiter;
        this.policyDAO = policyDAO;
        this.databaseConfig = databaseConfig;
        initializeDefaultPolicies();
    }

    private void initializeDefaultPolicies() {
        clientPolicies.put("client-analytics", RateLimitPolicy.createTokenBucketPolicy(
                "pol-analytics", "client-analytics", 10, 5, 1000
        ));
        clientPolicies.put("client-mobile", RateLimitPolicy.createSlidingWindowPolicy(
                "pol-mobile", "client-mobile", 5, 10000
        ));
    }

    /**
     * Resolves the active rate limit policy for a client.
     * 1. Checks local cache / registered policies.
     * 2. If not found, attempts to query MySQL via RateLimitPolicyDAO.
     * 3. Returns null if no active policy exists.
     *
     * @param clientId client identifier
     * @return active RateLimitPolicy or null
     */
    public RateLimitPolicy getPolicy(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return null;
        }

        String trimmedId = clientId.trim();

        // 1. Check in-memory registered/cached policy first
        RateLimitPolicy cached = clientPolicies.get(trimmedId);
        if (cached != null) {
            return cached;
        }

        // 2. Query active policy from MySQL via RateLimitPolicyDAO
        if (policyDAO != null && databaseConfig != null) {
            try (Connection connection = databaseConfig.getConnection()) {
                Optional<RateLimitPolicy> dbPolicy = policyDAO.findActivePolicy(connection, trimmedId);
                if (dbPolicy.isPresent()) {
                    RateLimitPolicy policy = dbPolicy.get();
                    clientPolicies.put(trimmedId, policy);
                    return policy;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, () -> "MySQL policy retrieval failed for client: " + trimmedId + " (" + e.getMessage() + ")");
            }
        }

        return null;
    }

    public void registerPolicy(RateLimitPolicy policy) {
        if (policy != null && policy.getClientId() != null) {
            clientPolicies.put(policy.getClientId(), policy);
        }
    }

    public RateLimitResult evaluate(String clientId, long currentTimeMillis) {
        RateLimitPolicy policy = getPolicy(clientId);
        if (policy == null) {
            return RateLimitResult.reject(0.0, null, "No rate limit policy found");
        }

        if (policy.getAlgorithmType() == AlgorithmType.SLIDING_WINDOW) {
            return slidingWindowLimiter.allowRequest(policy, currentTimeMillis);
        }

        return tokenBucketLimiter.allowRequest(policy, currentTimeMillis);
    }
}
