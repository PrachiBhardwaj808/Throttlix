package com.throttlix.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.throttlix.model.ApiKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ApiKeyDAOTest {

    private final ApiKeyDAO apiKeyDAO = new ApiKeyDAO();

    @Test
    void shouldCreateApiKeyAndReturnGeneratedKey() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet generatedKeys = mock(ResultSet.class);

        when(connection.prepareStatement(anyString(), anyInt())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        when(statement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(true);
        when(generatedKeys.getLong(1)).thenReturn(42L);

        assertEquals(42L, apiKeyDAO.createApiKey(connection, 1L, "mobile-primary", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
    }

    @Test
    void shouldFindClientByActiveKeyHash() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("client_name")).thenReturn("client-mobile");

        Optional<String> client = apiKeyDAO.findClientByActiveKeyHash(connection, "hash12345");
        assertTrue(client.isPresent());
        assertEquals("client-mobile", client.get());
    }

    @Test
    void shouldRevokeApiKeySuccessfully() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        boolean revoked = apiKeyDAO.revokeApiKey(connection, "hash12345");
        assertTrue(revoked);
    }

    @Test
    void shouldFindApiKeyByHash() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("api_key_id")).thenReturn(10L);
        when(resultSet.getLong("client_id")).thenReturn(2L);
        when(resultSet.getString("name")).thenReturn("prod-key");
        when(resultSet.getString("key_hash")).thenReturn("hash12345");
        when(resultSet.getBoolean("active")).thenReturn(true);
        when(resultSet.getTimestamp("created_at")).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(resultSet.getTimestamp("revoked_at")).thenReturn(null);

        Optional<ApiKey> apiKey = apiKeyDAO.findByKeyHash(connection, "hash12345");
        assertTrue(apiKey.isPresent());
        assertEquals(10L, apiKey.get().apiKeyId());
        assertEquals(2L, apiKey.get().clientId());
        assertEquals("hash12345", apiKey.get().keyHash());
        assertTrue(apiKey.get().active());
        assertNotNull(apiKey.get().createdAt());
    }

    @Test
    void shouldRejectInvalidInput() throws Exception {
        Connection connection = mock(Connection.class);
        assertThrows(IllegalArgumentException.class, () -> apiKeyDAO.createApiKey(null, 1L, "name", "hash"));
        assertThrows(IllegalArgumentException.class, () -> apiKeyDAO.createApiKey(connection, 1L, "  ", "hash"));
        assertThrows(IllegalArgumentException.class, () -> apiKeyDAO.createApiKey(connection, 1L, "name", "  "));
        assertFalse(apiKeyDAO.revokeApiKey(connection, "  "));
        assertTrue(apiKeyDAO.findClientByActiveKeyHash(connection, "").isEmpty());
    }
}
