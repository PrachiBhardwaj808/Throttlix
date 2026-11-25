package com.throttlix.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.throttlix.model.ApiKey;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
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
        Connection connection = mockConnectionWithGeneratedKey(42L);
        long keyId = apiKeyDAO.createApiKey(connection, 1L, "mobile-primary", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertEquals(42L, keyId);
    }

    @Test
    void shouldFindClientByActiveKeyHash() throws Exception {
        Connection connection = mockConnectionWithClientName("client-mobile");
        Optional<String> client = apiKeyDAO.findClientByActiveKeyHash(connection, "hash12345");
        assertTrue(client.isPresent());
        assertEquals("client-mobile", client.get());
    }

    @Test
    void shouldRevokeApiKeySuccessfully() throws Exception {
        Connection connection = mockConnectionWithUpdateCount(1);
        boolean revoked = apiKeyDAO.revokeApiKey(connection, "hash12345");
        assertTrue(revoked);
    }

    @Test
    void shouldFindApiKeyByHash() throws Exception {
        Connection connection = mockConnectionWithApiKeyRow(10L, 2L, "prod-key", "hash12345", true);
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
        Connection connection = mockBasicConnection();
        assertThrows(IllegalArgumentException.class, () -> apiKeyDAO.createApiKey(null, 1L, "name", "hash"));
        assertThrows(IllegalArgumentException.class, () -> apiKeyDAO.createApiKey(connection, 1L, "  ", "hash"));
        assertThrows(IllegalArgumentException.class, () -> apiKeyDAO.createApiKey(connection, 1L, "name", "  "));
        assertFalse(apiKeyDAO.revokeApiKey(connection, "  "));
        assertTrue(apiKeyDAO.findClientByActiveKeyHash(connection, "").isEmpty());
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private Connection mockBasicConnection() {
        return proxy(Connection.class, (p, m, a) -> null);
    }

    private Connection mockConnectionWithGeneratedKey(long key) {
        ResultSet keysRs = proxy(ResultSet.class, (p, m, a) -> switch (m.getName()) {
            case "next" -> true;
            case "getLong" -> key;
            case "close" -> null;
            default -> null;
        });

        PreparedStatement stmt = proxy(PreparedStatement.class, (p, m, a) -> switch (m.getName()) {
            case "setLong", "setString" -> null;
            case "executeUpdate" -> 1;
            case "getGeneratedKeys" -> keysRs;
            case "close" -> null;
            default -> null;
        });

        return proxy(Connection.class, (p, m, a) -> {
            if ("prepareStatement".equals(m.getName())) return stmt;
            return null;
        });
    }

    private Connection mockConnectionWithClientName(String clientName) {
        ResultSet rs = proxy(ResultSet.class, (p, m, a) -> switch (m.getName()) {
            case "next" -> true;
            case "getString" -> clientName;
            case "close" -> null;
            default -> null;
        });

        PreparedStatement stmt = proxy(PreparedStatement.class, (p, m, a) -> switch (m.getName()) {
            case "setString" -> null;
            case "executeQuery" -> rs;
            case "close" -> null;
            default -> null;
        });

        return proxy(Connection.class, (p, m, a) -> {
            if ("prepareStatement".equals(m.getName())) return stmt;
            return null;
        });
    }

    private Connection mockConnectionWithUpdateCount(int count) {
        PreparedStatement stmt = proxy(PreparedStatement.class, (p, m, a) -> switch (m.getName()) {
            case "setString" -> null;
            case "executeUpdate" -> count;
            case "close" -> null;
            default -> null;
        });

        return proxy(Connection.class, (p, m, a) -> {
            if ("prepareStatement".equals(m.getName())) return stmt;
            return null;
        });
    }

    private Connection mockConnectionWithApiKeyRow(long id, long clientId, String name, String hash, boolean active) {
        ResultSet rs = proxy(ResultSet.class, (p, m, a) -> switch (m.getName()) {
            case "next" -> true;
            case "getLong" -> "api_key_id".equals(a[0]) ? id : clientId;
            case "getString" -> "name".equals(a[0]) ? name : hash;
            case "getBoolean" -> active;
            case "getTimestamp" -> "created_at".equals(a[0]) ? new Timestamp(System.currentTimeMillis()) : null;
            case "close" -> null;
            default -> null;
        });

        PreparedStatement stmt = proxy(PreparedStatement.class, (p, m, a) -> switch (m.getName()) {
            case "setString" -> null;
            case "executeQuery" -> rs;
            case "close" -> null;
            default -> null;
        });

        return proxy(Connection.class, (p, m, a) -> {
            if ("prepareStatement".equals(m.getName())) return stmt;
            return null;
        });
    }
}
