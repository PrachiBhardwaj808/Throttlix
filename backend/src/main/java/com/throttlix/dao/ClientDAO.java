package com.throttlix.dao;

import com.throttlix.model.Client;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * Data Access Object for managing client accounts in MySQL.
 */
public class ClientDAO {

    private static final String INSERT_CLIENT_SQL = """
            INSERT INTO clients (name, active)
            VALUES (?, TRUE)
            """;

    private static final String FIND_CLIENT_BY_NAME_SQL = """
            SELECT client_id, name, active, created_at
            FROM clients
            WHERE name = ?
            """;

    private static final String FIND_CLIENT_BY_ID_SQL = """
            SELECT client_id, name, active, created_at
            FROM clients
            WHERE client_id = ?
            """;

    private static final String IS_CLIENT_ACTIVE_SQL = """
            SELECT active
            FROM clients
            WHERE name = ?
            """;

    private static final String UPDATE_CLIENT_STATUS_SQL = """
            UPDATE clients
            SET active = ?
            WHERE client_id = ?
            """;

    /**
     * Creates a new client organization in MySQL.
     * Uses JDBC Statement.RETURN_GENERATED_KEYS to retrieve the AUTO_INCREMENT primary key.
     *
     * @param connection active JDBC connection
     * @param name       unique client identifier
     * @return generated client_id
     * @throws SQLException if execution or key retrieval fails
     */
    public long createClient(Connection connection, String name) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Client name is required");
        }

        try (PreparedStatement statement = connection.prepareStatement(INSERT_CLIENT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name.trim());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("Failed to retrieve generated client_id for client: " + name);
            }
        }
    }

    /**
     * Retrieves a client record by name.
     */
    public Optional<Client> findClientByName(Connection connection, String name) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Client name is required");
        }

        try (PreparedStatement statement = connection.prepareStatement(FIND_CLIENT_BY_NAME_SQL)) {
            statement.setString(1, name.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapClient(resultSet));
            }
        }
    }

    /**
     * Retrieves a client record by primary key ID.
     */
    public Optional<Client> findClientById(Connection connection, long clientId) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required");
        }

        try (PreparedStatement statement = connection.prepareStatement(FIND_CLIENT_BY_ID_SQL)) {
            statement.setLong(1, clientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapClient(resultSet));
            }
        }
    }

    /**
     * Checks if a client exists and is active.
     */
    public Optional<Boolean> isClientActive(Connection connection, String name) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Client name is required");
        }

        try (PreparedStatement statement = connection.prepareStatement(IS_CLIENT_ACTIVE_SQL)) {
            statement.setString(1, name.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(resultSet.getBoolean("active"));
            }
        }
    }

    /**
     * Updates the active status of a client.
     */
    public boolean setClientActive(Connection connection, long clientId, boolean active) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Database connection is required");
        }

        try (PreparedStatement statement = connection.prepareStatement(UPDATE_CLIENT_STATUS_SQL)) {
            statement.setBoolean(1, active);
            statement.setLong(2, clientId);
            return statement.executeUpdate() > 0;
        }
    }

    private Client mapClient(ResultSet resultSet) throws SQLException {
        long clientId = resultSet.getLong("client_id");
        String name = resultSet.getString("name");
        boolean active = resultSet.getBoolean("active");
        Timestamp timestamp = resultSet.getTimestamp("created_at");
        Instant createdAt = timestamp != null ? timestamp.toInstant() : Instant.now();
        return new Client(clientId, name, active, createdAt);
    }
}
