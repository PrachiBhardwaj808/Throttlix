package com.throttlix.dao;

import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Optional;

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

    private static final String INSERT_POLICY_SQL = """
            INSERT INTO rate_limit_policies (
                name, algorithm, request_limit, window_seconds,
                refill_tokens, refill_duration_millis, enabled
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String ASSIGN_POLICY_SQL = """
            INSERT INTO client_policies (client_id, policy_id)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE policy_id = VALUES(policy_id), assigned_at = CURRENT_TIMESTAMP
            """;

    private static final String UPDATE_POLICY_SQL = """
            UPDATE rate_limit_policies
            SET request_limit = ?,
                window_seconds = ?,
                refill_tokens = ?,
                refill_duration_millis = ?
            WHERE policy_id = ?
            """;

    private static final String FIND_POLICY_BY_ID_SQL = """
            SELECT policy_id,
                   name,
                   algorithm,
                   request_limit,
                   window_seconds,
                   refill_tokens,
                   refill_duration_millis,
                   enabled
            FROM rate_limit_policies
            WHERE policy_id = ?
            """;

    public Optional<RateLimitPolicy> findActivePolicy(Connection connection, String clientId) throws SQLException {
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

    public long createPolicy(Connection connection, String name, RateLimitPolicy policy) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Policy name is required");
        }
        if (policy == null) {
            throw new IllegalArgumentException("Policy object is required");
        }

        try (PreparedStatement statement = connection.prepareStatement(INSERT_POLICY_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name.trim());
            statement.setString(2, policy.getAlgorithmType().name());
            statement.setInt(3, policy.getLimit());

            if (policy.getAlgorithmType() == AlgorithmType.SLIDING_WINDOW) {
                int windowSeconds = (int) Math.max(1L, policy.getWindowDurationMillis() / 1000L);
                statement.setInt(4, windowSeconds);
                statement.setNull(5, Types.INTEGER);
                statement.setNull(6, Types.BIGINT);
            } else {
                statement.setInt(4, 1);
                statement.setInt(5, policy.getRefillTokens());
                statement.setLong(6, policy.getRefillDurationMillis());
            }

            statement.setBoolean(7, true);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Failed to obtain generated policy_id");
            }
        }
    }

    public boolean assignPolicyToClient(Connection connection, long clientId, long policyId) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required");
        }

        try (PreparedStatement statement = connection.prepareStatement(ASSIGN_POLICY_SQL)) {
            statement.setLong(1, clientId);
            statement.setLong(2, policyId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updatePolicy(Connection connection, long policyId, RateLimitPolicy policy) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required");
        }
        if (policy == null) {
            throw new IllegalArgumentException("Policy object is required");
        }

        try (PreparedStatement statement = connection.prepareStatement(UPDATE_POLICY_SQL)) {
            statement.setInt(1, policy.getLimit());
            if (policy.getAlgorithmType() == AlgorithmType.SLIDING_WINDOW) {
                int windowSeconds = (int) Math.max(1L, policy.getWindowDurationMillis() / 1000L);
                statement.setInt(2, windowSeconds);
                statement.setNull(3, Types.INTEGER);
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setInt(2, 1);
                statement.setInt(3, policy.getRefillTokens());
                statement.setLong(4, policy.getRefillDurationMillis());
            }
            statement.setLong(5, policyId);
            return statement.executeUpdate() > 0;
        }
    }

    public Optional<RateLimitPolicy> findPolicyById(Connection connection, long policyId, String clientName) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required");
        }

        try (PreparedStatement statement = connection.prepareStatement(FIND_POLICY_BY_ID_SQL)) {
            statement.setLong(1, policyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                String pId = String.valueOf(resultSet.getLong("policy_id"));
                String cName = (clientName != null && !clientName.isBlank()) ? clientName : "client-" + pId;
                AlgorithmType algorithmType = AlgorithmType.valueOf(resultSet.getString("algorithm"));
                int limit = resultSet.getInt("request_limit");

                if (algorithmType == AlgorithmType.SLIDING_WINDOW) {
                    long windowMillis = Math.multiplyExact(resultSet.getLong("window_seconds"), 1000L);
                    return Optional.of(RateLimitPolicy.createSlidingWindowPolicy(pId, cName, limit, windowMillis));
                }

                int refillTokens = resultSet.getInt("refill_tokens");
                long refillDurationMillis = resultSet.getLong("refill_duration_millis");
                return Optional.of(RateLimitPolicy.createTokenBucketPolicy(pId, cName, limit, refillTokens, refillDurationMillis));
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
            long windowMillis = Math.multiplyExact(resultSet.getLong("window_seconds"), 1_000L);
            return RateLimitPolicy.createSlidingWindowPolicy(policyId, clientId, limit, windowMillis);
        }

        int refillTokens = resultSet.getInt("refill_tokens");
        if (resultSet.wasNull()) {
            throw new SQLException("Token Bucket policy is missing refill_tokens");
        }
        long refillDurationMillis = resultSet.getLong("refill_duration_millis");
        if (resultSet.wasNull()) {
            throw new SQLException("Token Bucket policy is missing refill_duration_millis");
        }
        return RateLimitPolicy.createTokenBucketPolicy(policyId, clientId, limit, refillTokens, refillDurationMillis);
    }
}
