package com.throttlix.dao;

import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Loads the active rate-limit policy assigned to a client. */
public class RateLimitPolicyDAO {

    private static final String FIND_ACTIVE_POLICY_SQL = """
            SELECT p.policy_id,
                   c.name AS client_name,
                   p.algorithm,
                   p.request_limit,
                   p.window_seconds,
                   p.refill_tokens,
                   p.refill_duration_millis,
                   p.enabled
            FROM clients c
            JOIN client_policies cp ON cp.client_id = c.client_id
            JOIN rate_limit_policies p ON p.policy_id = cp.policy_id
            WHERE c.name = ?
              AND c.active = TRUE
              AND p.enabled = TRUE
            """;

    /**
     * Finds the active policy assigned to a client.
     * The caller owns the connection and is responsible for closing it.
     */
    public Optional<RateLimitPolicy> findActivePolicy(Connection connection, String clientId)
            throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Client ID is required");
        }

        try (PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE_POLICY_SQL)) {
            statement.setString(1, clientId.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapPolicy(resultSet));
            }
        }
    }

    private RateLimitPolicy mapPolicy(ResultSet resultSet) throws SQLException {
        String policyId = String.valueOf(resultSet.getLong("policy_id"));
        String clientId = resultSet.getString("client_name");
        AlgorithmType algorithmType;
        try {
            algorithmType = AlgorithmType.valueOf(resultSet.getString("algorithm"));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new SQLException("Policy contains an unsupported algorithm", exception);
        }

        int limit = resultSet.getInt("request_limit");
        if (algorithmType == AlgorithmType.SLIDING_WINDOW) {
            long windowMillis;
            try {
                windowMillis = Math.multiplyExact(resultSet.getLong("window_seconds"), 1_000L);
            } catch (ArithmeticException exception) {
                throw new SQLException("Policy window duration is too large", exception);
            }
            return RateLimitPolicy.createSlidingWindowPolicy(
                    policyId, clientId, limit, windowMillis);
        }

        int refillTokens = resultSet.getInt("refill_tokens");
        if (resultSet.wasNull()) {
            throw new SQLException("Token Bucket policy is missing refill_tokens");
        }
        long refillDurationMillis = resultSet.getLong("refill_duration_millis");
        if (resultSet.wasNull()) {
            throw new SQLException("Token Bucket policy is missing refill_duration_millis");
        }
        return RateLimitPolicy.createTokenBucketPolicy(
                policyId, clientId, limit, refillTokens, refillDurationMillis);
    }
}
