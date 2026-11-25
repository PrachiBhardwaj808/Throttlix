package com.throttlix.dao;

import com.throttlix.model.ApiKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public class ApiKeyDAO {

    private static final String INSERT_API_KEY_SQL = """
            INSERT INTO api_keys (client_id, name, key_hash, active)
            VALUES (?, ?, ?, TRUE)
            """;

    private static final String VERIFY_ACTIVE_KEY_SQL = """
            SELECT c.name AS client_name
            FROM api_keys k
            JOIN clients c ON c.client_id = k.client_id
            WHERE k.key_hash = ?
              AND k.active = TRUE
              AND c.active = TRUE
            """;

    private static final String REVOKE_API_KEY_SQL = """
            UPDATE api_keys
            SET active = FALSE,
                revoked_at = CURRENT_TIMESTAMP
            WHERE key_hash = ?
              AND active = TRUE
            """;

    private static final String FIND_KEY_BY_HASH_SQL = """
            SELECT api_key_id, client_id, name, key_hash, active, created_at, revoked_at
            FROM api_keys
            WHERE key_hash = ?
            """;

    public long createApiKey(Connection connection, long clientId, String keyName, String keyHash) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required");
        }
        if (keyName == null || keyName.isBlank()) {
            throw new IllegalArgumentException("Key name is required");
        }
        if (keyHash == null || keyHash.isBlank()) {
            throw new IllegalArgumentException("Key hash is required");
        }

        try (PreparedStatement statement = connection.prepareStatement(INSERT_API_KEY_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, clientId);
            statement.setString(2, keyName.trim());
            statement.setString(3, keyHash.trim());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Failed to retrieve generated api_key_id");
            }
        }
    }

    public Optional<String> findClientByActiveKeyHash(Connection connection, String keyHash) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required");
        }
        if (keyHash == null || keyHash.isBlank()) {
            return Optional.empty();
        }

        try (PreparedStatement statement = connection.prepareStatement(VERIFY_ACTIVE_KEY_SQL)) {
            statement.setString(1, keyHash.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(resultSet.getString("client_name"));
            }
        }
    }

    public boolean revokeApiKey(Connection connection, String keyHash) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required");
        }
        if (keyHash == null || keyHash.isBlank()) {
            return false;
        }

        try (PreparedStatement statement = connection.prepareStatement(REVOKE_API_KEY_SQL)) {
            statement.setString(1, keyHash.trim());
            return statement.executeUpdate() > 0;
        }
    }

    public Optional<ApiKey> findByKeyHash(Connection connection, String keyHash) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required");
        }
        if (keyHash == null || keyHash.isBlank()) {
            return Optional.empty();
        }

        try (PreparedStatement statement = connection.prepareStatement(FIND_KEY_BY_HASH_SQL)) {
            statement.setString(1, keyHash.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapApiKey(resultSet));
            }
        }
    }

    private ApiKey mapApiKey(ResultSet resultSet) throws SQLException {
        long apiKeyId = resultSet.getLong("api_key_id");
        long clientId = resultSet.getLong("client_id");
        String name = resultSet.getString("name");
        String keyHash = resultSet.getString("key_hash");
        boolean active = resultSet.getBoolean("active");
        Timestamp createdTs = resultSet.getTimestamp("created_at");
        Timestamp revokedTs = resultSet.getTimestamp("revoked_at");
        return new ApiKey(
                apiKeyId,
                clientId,
                name,
                keyHash,
                active,
                createdTs != null ? createdTs.toInstant() : Instant.now(),
                revokedTs != null ? revokedTs.toInstant() : null
        );
    }
}
